package com.employeehub.dto;

import java.util.List;

/**
 * Answer to a natural-language question. The answer text is written by the backend from real query
 * results; the AI only chose the filters (shown in interpretedAs).
 */
public record AskResponse(
        boolean answerable,
        String answer,
        Long totalMatches,
        List<EmployeeDto> employees,
        List<String> interpretedAs) {

    public static AskResponse cannotAnswer(String reason) {
        return new AskResponse(false, reason, null, List.of(), List.of());
    }
}
