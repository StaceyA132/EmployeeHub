package com.employeehub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AskRequest(
        @NotBlank(message = "must not be blank")
        @Size(max = 300, message = "must be 300 characters or fewer")
        String question) {
}
