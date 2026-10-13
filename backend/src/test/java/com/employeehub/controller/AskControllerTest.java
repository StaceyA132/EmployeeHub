package com.employeehub.controller;

import com.employeehub.ai.AiQueryInterpreter;
import com.employeehub.ai.AiServiceException;
import com.employeehub.ai.QuestionInterpretation;
import com.employeehub.model.Role;
import com.employeehub.security.JwtUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** POST /api/ask through the real security chain, with the AI mocked. */
@SpringBootTest
@AutoConfigureMockMvc
class AskControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtUtils jwtUtils;

    @MockBean
    private AiQueryInterpreter interpreter;

    private String token(Role role) {
        return "Bearer " + jwtUtils.generateToken(role == Role.ROLE_ADMIN ? "admin" : "manager", List.of(role.name()));
    }

    private ResultActions ask(String question) throws Exception {
        return mvc.perform(post("/api/ask").header("Authorization", token(Role.ROLE_ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\":" + (question == null ? "null" : "\"" + question + "\"") + "}"));
    }

    private static QuestionInterpretation countEveryone() {
        QuestionInterpretation i = new QuestionInterpretation();
        i.setAnswerable(true);
        i.setResultType("COUNT");
        return i;
    }

    private void aiFailsWith(AiServiceException.Kind kind) {
        // doThrow form, because when(...) would invoke the previously stubbed throw
        doThrow(new AiServiceException(kind, "internal detail")).when(interpreter).interpret(anyString(), any());
    }

    @Test
    void requiresLogin() throws Exception {
        mvc.perform(post("/api/ask").contentType(MediaType.APPLICATION_JSON).content("{\"question\":\"hi\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void answersAQuestion() throws Exception {
        when(interpreter.interpret(anyString(), any())).thenReturn(countEveryone());

        ask("how many people work here?")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answerable").value(true))
                .andExpect(jsonPath("$.answer").value(org.hamcrest.Matchers.startsWith("There are")))
                .andExpect(jsonPath("$.totalMatches").isNumber())
                .andExpect(jsonPath("$.interpretedAs[0]").value("Count only"));
    }

    @Test
    void cantAnswerIs200WithReason() throws Exception {
        QuestionInterpretation i = new QuestionInterpretation();
        i.setAnswerable(false);
        i.setReason("Performance isn't tracked in the employee directory.");
        when(interpreter.interpret(anyString(), any())).thenReturn(i);

        ask("who is the best performer?")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answerable").value(false))
                .andExpect(jsonPath("$.answer").value("Performance isn't tracked in the employee directory."));
    }

    @Test
    void blankOrTooLongQuestionIs400() throws Exception {
        ask("   ").andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("question must not be blank"));
        ask(null).andExpect(status().isBadRequest());
        ask("x".repeat(301)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("question must be 300 characters or fewer"));
    }

    @Test
    void aiFailuresMapToStatusesWithoutLeakingDetails() throws Exception {
        aiFailsWith(AiServiceException.Kind.NOT_CONFIGURED);
        ask("q").andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("The Ask feature isn't set up on this server."));

        aiFailsWith(AiServiceException.Kind.RATE_LIMITED);
        ask("q").andExpect(status().isServiceUnavailable());

        aiFailsWith(AiServiceException.Kind.TIMEOUT);
        ask("q").andExpect(status().isGatewayTimeout());

        aiFailsWith(AiServiceException.Kind.PROVIDER_ERROR);
        ask("q").andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("internal detail"))));

        aiFailsWith(AiServiceException.Kind.INVALID_RESPONSE);
        ask("q").andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("I couldn't understand that question. Try rephrasing it."));
    }
}
