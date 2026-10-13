package com.employeehub.controller;

import com.employeehub.ai.AskService;
import com.employeehub.dto.AskRequest;
import com.employeehub.dto.AskResponse;
import com.employeehub.security.SecurityRoles;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Plain-English questions about employees. Any signed-in user may ask; salary access follows their role. */
@RestController
@RequestMapping("/api/ask")
public class AskController {

    private final AskService askService;

    public AskController(AskService askService) {
        this.askService = askService;
    }

    @PostMapping
    public ResponseEntity<AskResponse> ask(@Valid @RequestBody AskRequest request, Authentication auth) {
        return ResponseEntity.ok(askService.ask(request.question().trim(), SecurityRoles.canViewSalary(auth)));
    }
}
