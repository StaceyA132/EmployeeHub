package com.employeehub.query;

import com.employeehub.repository.DepartmentRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/**
 * Checks and normalizes an {@link EmployeeQuery}. Nothing reaches the database until this passes,
 * so a confused (or manipulated) AI response can't produce a nonsensical or unauthorized query.
 */
@Component
public class EmployeeQueryValidator {

    public static final List<String> STATUSES = List.of("Active", "On Leave", "Terminated");
    private static final int MAX_TEXT_LENGTH = 100;

    private final DepartmentRepository departmentRepository;

    public EmployeeQueryValidator(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    /**
     * Validates the query in place, normalizing values (trimmed text, canonical department/status names).
     *
     * @param canViewSalary whether the caller may filter or sort by salary
     * @throws InvalidQueryException with a user-readable message when the query is rejected
     */
    public void validate(EmployeeQuery query, boolean canViewSalary) {
        if (query.usesSalary() && !canViewSalary) {
            throw new InvalidQueryException("Salary information is only available to admins and managers.");
        }

        query.setDepartment(blankToNull(query.getDepartment()));
        query.setStatus(blankToNull(query.getStatus()));
        query.setPositionContains(checkLength(blankToNull(query.getPositionContains())));
        query.setNameContains(checkLength(blankToNull(query.getNameContains())));

        if (query.getDepartment() != null) {
            String requested = query.getDepartment();
            String canonical = departmentRepository.findByNameIgnoreCase(requested)
                    .orElseThrow(() -> new InvalidQueryException("There is no department called \"" + requested + "\"."))
                    .getName();
            query.setDepartment(canonical);
        }

        if (query.getStatus() != null) {
            String requested = query.getStatus();
            query.setStatus(STATUSES.stream()
                    .filter(s -> s.equalsIgnoreCase(requested))
                    .findFirst()
                    .orElseThrow(() -> new InvalidQueryException(
                            "Unknown status \"" + requested + "\". Use one of: " + String.join(", ", STATUSES) + ".")));
        }

        LocalDate from = query.getHiredOnOrAfter();
        LocalDate to = query.getHiredOnOrBefore();
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidQueryException("The start of the hire-date range is after the end.");
        }

        BigDecimal min = query.getMinSalary();
        BigDecimal max = query.getMaxSalary();
        if ((min != null && min.signum() < 0) || (max != null && max.signum() < 0)) {
            throw new InvalidQueryException("Salary amounts can't be negative.");
        }
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new InvalidQueryException("The minimum salary is higher than the maximum.");
        }

        if (query.getResultType() == null) {
            query.setResultType(EmployeeQuery.ResultType.LIST);
        }
        if (query.getSortBy() == null) {
            query.setSortBy(EmployeeQuery.SortField.NAME);
        }
        if (query.getSortDirection() == null) {
            query.setSortDirection(EmployeeQuery.SortDirection.ASC);
        }
        Integer limit = query.getLimit();
        if (limit == null || limit < 1) {
            query.setLimit(EmployeeQuery.DEFAULT_LIMIT);
        } else if (limit > EmployeeQuery.MAX_LIMIT) {
            query.setLimit(EmployeeQuery.MAX_LIMIT);
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String checkLength(String value) {
        if (value != null && value.length() > MAX_TEXT_LENGTH) {
            throw new InvalidQueryException("Search text is too long.");
        }
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }
}
