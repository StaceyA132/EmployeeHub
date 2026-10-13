package com.employeehub.dto;

import java.util.List;

/** Result of running an EmployeeQuery: the total number of matches and (up to the limit) the matching employees. */
public record EmployeeSearchResult(long totalMatches, List<EmployeeDto> employees) {
}
