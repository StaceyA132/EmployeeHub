package com.employeehub.ai;

import com.employeehub.dto.AskResponse;
import com.employeehub.dto.EmployeeDto;
import com.employeehub.model.Department;
import com.employeehub.model.Employee;
import com.employeehub.repository.DepartmentRepository;
import com.employeehub.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** AskService end to end against H2, with the AI replaced by a Mockito mock. */
@SpringBootTest
@Transactional
class AskServiceTest {

    @MockBean
    private AiQueryInterpreter interpreter;

    @Autowired
    private AskService askService;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    private final LocalDate today = LocalDate.now();

    @BeforeEach
    void seed() {
        Department sales = departmentRepository.save(new Department("Ask Sales"));
        employeeRepository.save(new Employee("Ana", "Lopez", "ana@ask-test.com", "Account Executive",
                BigDecimal.valueOf(90_000), today.minusDays(10), "Active", sales));
        employeeRepository.save(new Employee("Ben", "Ortiz", "ben@ask-test.com", "Sales Manager",
                BigDecimal.valueOf(130_000), today.minusDays(400), "Active", sales));
    }

    /** An answerable interpretation with every filter empty, like the model returns. */
    private static QuestionInterpretation blank() {
        QuestionInterpretation i = new QuestionInterpretation();
        i.setAnswerable(true);
        i.setReason("");
        i.setResultType("LIST");
        i.setDepartment("");
        i.setStatus("");
        i.setPositionContains("");
        i.setNameContains("");
        i.setHiredOnOrAfter("");
        i.setHiredOnOrBefore("");
        i.setMinSalary("");
        i.setMaxSalary("");
        i.setSortBy("NAME");
        i.setSortDirection("ASC");
        i.setLimit(20);
        return i;
    }

    @Test
    void countQuestionGetsWrittenAnswerFromRealData() {
        QuestionInterpretation i = blank();
        i.setResultType("COUNT");
        i.setDepartment("ask sales");
        when(interpreter.interpret(anyString(), any())).thenReturn(i);

        AskResponse response = askService.ask("how many people are in sales?", false);

        assertThat(response.answerable()).isTrue();
        assertThat(response.answer()).isEqualTo("There are 2 employees in Ask Sales.");
        assertThat(response.totalMatches()).isEqualTo(2);
        assertThat(response.employees()).isEmpty();
        assertThat(response.interpretedAs()).containsExactly("Department: Ask Sales", "Count only");
    }

    @Test
    void sendsOnlyDateDepartmentsAndPermissionToTheAi() {
        when(interpreter.interpret(anyString(), any())).thenReturn(blank());

        askService.ask("who works here?", false);

        ArgumentCaptor<InterpretationContext> context = ArgumentCaptor.forClass(InterpretationContext.class);
        verify(interpreter).interpret(org.mockito.ArgumentMatchers.eq("who works here?"), context.capture());
        assertThat(context.getValue().today()).isEqualTo(today);
        assertThat(context.getValue().departments()).contains("Ask Sales");
        assertThat(context.getValue().canViewSalary()).isFalse();
    }

    @Test
    void listQuestionHidesSalariesFromReadOnlyUsers() {
        QuestionInterpretation i = blank();
        i.setDepartment("Ask Sales");
        i.setHiredOnOrAfter(today.minusDays(90).toString());
        i.setSortBy("HIRE_DATE");
        i.setSortDirection("DESC");
        when(interpreter.interpret(anyString(), any())).thenReturn(i);

        AskResponse viewer = askService.ask("who joined in the last 3 months?", false);
        assertThat(viewer.answer()).startsWith("Found 1 employee in Ask Sales, hired on or after");
        assertThat(viewer.employees()).extracting(EmployeeDto::getFirstName).containsExactly("Ana");
        assertThat(viewer.employees()).allSatisfy(e -> assertThat(e.getSalary()).isNull());
        assertThat(viewer.interpretedAs()).contains("Sorted by hire date (newest first)");

        AskResponse manager = askService.ask("who joined in the last 3 months?", true);
        assertThat(manager.employees()).allSatisfy(e -> assertThat(e.getSalary()).isNotNull());
    }

    @Test
    void topNAnswerSaysTopRatherThanFirst() {
        QuestionInterpretation i = blank();
        i.setDepartment("Ask Sales");
        i.setSortBy("HIRE_DATE");
        i.setSortDirection("ASC");
        i.setLimit(1);
        when(interpreter.interpret(anyString(), any())).thenReturn(i);

        AskResponse response = askService.ask("who has been here the longest?", true);

        assertThat(response.answer()).isEqualTo(
                "Found 2 employees in Ask Sales. Showing the top 1 by hire date (oldest first).");
        assertThat(response.employees()).extracting(EmployeeDto::getFirstName).containsExactly("Ben");
    }

    @Test
    void unanswerableQuestionReturnsTheModelsReason() {
        QuestionInterpretation i = blank();
        i.setAnswerable(false);
        i.setReason("Performance isn't tracked in the employee directory.");
        when(interpreter.interpret(anyString(), any())).thenReturn(i);

        AskResponse response = askService.ask("who is our best performer?", true);

        assertThat(response.answerable()).isFalse();
        assertThat(response.answer()).isEqualTo("Performance isn't tracked in the employee directory.");
    }

    @Test
    void validationProblemsBecomeFriendlyCantAnswerMessages() {
        QuestionInterpretation unknownDept = blank();
        unknownDept.setDepartment("Sails");
        when(interpreter.interpret(anyString(), any())).thenReturn(unknownDept);
        assertThat(askService.ask("how many in sails?", true).answer())
                .isEqualTo("There is no department called \"Sails\".");

        QuestionInterpretation salaryForViewer = blank();
        salaryForViewer.setMinSalary("100000");
        when(interpreter.interpret(anyString(), any())).thenReturn(salaryForViewer);
        AskResponse response = askService.ask("who earns over 100k?", false);
        assertThat(response.answerable()).isFalse();
        assertThat(response.answer()).contains("only available to admins and managers");
    }

    @Test
    void invalidAiOutputIsRetriedOnce() {
        QuestionInterpretation badDate = blank();
        badDate.setHiredOnOrAfter("last spring");
        QuestionInterpretation good = blank();
        good.setResultType("COUNT");
        when(interpreter.interpret(anyString(), any())).thenReturn(badDate).thenReturn(good);

        AskResponse response = askService.ask("how many people work here?", true);

        assertThat(response.answerable()).isTrue();
        verify(interpreter, times(2)).interpret(anyString(), any());
    }

    @Test
    void invalidAiOutputTwiceFails() {
        when(interpreter.interpret(anyString(), any())).thenThrow(
                new AiServiceException(AiServiceException.Kind.INVALID_RESPONSE, "bad json"));

        assertThatThrownBy(() -> askService.ask("q", true))
                .isInstanceOf(AiServiceException.class)
                .satisfies(e -> assertThat(((AiServiceException) e).getKind()).isEqualTo(AiServiceException.Kind.INVALID_RESPONSE));
        verify(interpreter, times(2)).interpret(anyString(), any());
    }

    @Test
    void providerFailuresAreNotRetriedHere() {
        when(interpreter.interpret(anyString(), any())).thenThrow(
                new AiServiceException(AiServiceException.Kind.TIMEOUT, "slow"));

        assertThatThrownBy(() -> askService.ask("q", true)).isInstanceOf(AiServiceException.class);
        verify(interpreter, times(1)).interpret(anyString(), any());
    }
}
