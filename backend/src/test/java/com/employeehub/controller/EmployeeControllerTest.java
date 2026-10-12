package com.employeehub.controller;

import com.employeehub.model.AppUser;
import com.employeehub.model.Role;
import com.employeehub.repository.UserRepository;
import com.employeehub.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end tests through the real security filter chain, using JWTs like the frontend does.
 * Admin and manager accounts come from DataInitializer.
 */
@SpringBootTest
@AutoConfigureMockMvc
class EmployeeControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String managerToken;
    private String viewerToken;

    @BeforeEach
    void setUp() {
        adminToken = bearer("admin", Role.ROLE_ADMIN);
        managerToken = bearer("manager", Role.ROLE_MANAGER);

        if (userRepository.findByUsername("viewer").isEmpty()) {
            userRepository.save(new AppUser("viewer", passwordEncoder.encode("viewer123"), Set.of(Role.ROLE_USER)));
        }
        viewerToken = bearer("viewer", Role.ROLE_USER);
    }

    private String bearer(String username, Role role) {
        return "Bearer " + jwtUtils.generateToken(username, List.of(role.name()));
    }

    private String employeeJson(String email) {
        return """
                {"firstName":"Test","lastName":"User","email":"%s","position":"QA Engineer",
                 "salary":65000,"hireDate":"2026-01-15","status":"Active","departmentName":"Engineering"}
                """.formatted(email);
    }

    private String uniqueEmail() {
        return "test." + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }

    @Test
    void loginWithSeededAdminReturnsToken() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void requestWithoutTokenIs401() throws Exception {
        mvc.perform(get("/api/employees")).andExpect(status().isUnauthorized());
    }

    @Test
    void readOnlyUserCannotCreateEmployee() throws Exception {
        mvc.perform(post("/api/employees").header("Authorization", viewerToken)
                        .contentType(MediaType.APPLICATION_JSON).content(employeeJson(uniqueEmail())))
                .andExpect(status().isForbidden());
    }

    @Test
    void managerCanCreateEmployee() throws Exception {
        mvc.perform(post("/api/employees").header("Authorization", managerToken)
                        .contentType(MediaType.APPLICATION_JSON).content(employeeJson(uniqueEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    void invalidEmployeeIs400WithMessage() throws Exception {
        mvc.perform(post("/api/employees").header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"firstName\":\"\",\"email\":\"nope\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void duplicateEmailIs409() throws Exception {
        String email = uniqueEmail();
        mvc.perform(post("/api/employees").header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(employeeJson(email)))
                .andExpect(status().isOk());

        mvc.perform(post("/api/employees").header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(employeeJson(email.toUpperCase())))
                .andExpect(status().isConflict());
    }

    @Test
    void missingEmployeeIs404() throws Exception {
        mvc.perform(get("/api/employees/999999").header("Authorization", adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void salaryVisibleToManagerButHiddenFromReadOnlyUser() throws Exception {
        mvc.perform(get("/api/employees").header("Authorization", managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].salary").isNumber());

        mvc.perform(get("/api/employees").header("Authorization", viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].salary").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.nullValue())));
    }

    @Test
    void onlyAdminCanRegisterUsers() throws Exception {
        String body = "{\"username\":\"new_" + UUID.randomUUID().toString().substring(0, 8) + "\",\"password\":\"Password123\"}";

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/register").header("Authorization", managerToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/register").header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("ROLE_USER"));
    }
}
