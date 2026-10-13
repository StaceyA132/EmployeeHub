package com.employeehub.ai;

import com.employeehub.dto.EmployeeSearchResult;
import com.employeehub.query.EmployeeQuery;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Writes answers and "interpreted as" labels from a validated query and its real results. */
final class AnswerFormatter {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US);

    private AnswerFormatter() {
    }

    static String answer(EmployeeQuery q, EmployeeSearchResult result) {
        long total = result.totalMatches();
        String scope = describe(q);
        String people = total == 1 ? "1 employee" : total + " employees";

        if (q.getResultType() == EmployeeQuery.ResultType.COUNT) {
            return scope.isEmpty()
                    ? "There " + (total == 1 ? "is " : "are ") + people + " in total."
                    : "There " + (total == 1 ? "is " : "are ") + people + " " + scope + ".";
        }
        if (total == 0) {
            return scope.isEmpty() ? "There are no employees yet." : "No employees found " + scope + ".";
        }
        String found = "Found " + people + (scope.isEmpty() ? "" : " " + scope) + ".";
        int shown = result.employees().size();
        return shown < total ? found + " Showing the first " + shown + "." : found;
    }

    /** Short labels describing what was searched, e.g. "Department: Sales". */
    static List<String> interpretedAs(EmployeeQuery q) {
        List<String> labels = new ArrayList<>();
        if (q.getDepartment() != null) labels.add("Department: " + q.getDepartment());
        if (q.getStatus() != null) labels.add("Status: " + q.getStatus());
        if (q.getPositionContains() != null) labels.add("Position contains: " + q.getPositionContains());
        if (q.getNameContains() != null) labels.add("Name contains: " + q.getNameContains());
        if (q.getHiredOnOrAfter() != null) labels.add("Hired on or after: " + date(q.getHiredOnOrAfter()));
        if (q.getHiredOnOrBefore() != null) labels.add("Hired on or before: " + date(q.getHiredOnOrBefore()));
        if (q.getMinSalary() != null) labels.add("Salary at least: " + money(q.getMinSalary()));
        if (q.getMaxSalary() != null) labels.add("Salary at most: " + money(q.getMaxSalary()));
        if (q.getResultType() == EmployeeQuery.ResultType.COUNT) {
            labels.add("Count only");
        } else if (q.getSortBy() != EmployeeQuery.SortField.NAME || q.getSortDirection() != EmployeeQuery.SortDirection.ASC) {
            labels.add("Sorted by " + sortLabel(q));
        }
        return labels;
    }

    // e.g. "in Sales, with status Active, hired on or after Jul 13, 2026"
    private static String describe(EmployeeQuery q) {
        List<String> parts = new ArrayList<>();
        if (q.getDepartment() != null) parts.add("in " + q.getDepartment());
        if (q.getStatus() != null) parts.add("with status " + q.getStatus());
        if (q.getPositionContains() != null) parts.add("whose position contains \"" + q.getPositionContains() + "\"");
        if (q.getNameContains() != null) parts.add("whose name contains \"" + q.getNameContains() + "\"");
        if (q.getHiredOnOrAfter() != null && q.getHiredOnOrBefore() != null) {
            parts.add("hired between " + date(q.getHiredOnOrAfter()) + " and " + date(q.getHiredOnOrBefore()));
        } else if (q.getHiredOnOrAfter() != null) {
            parts.add("hired on or after " + date(q.getHiredOnOrAfter()));
        } else if (q.getHiredOnOrBefore() != null) {
            parts.add("hired on or before " + date(q.getHiredOnOrBefore()));
        }
        if (q.getMinSalary() != null) parts.add("earning at least " + money(q.getMinSalary()));
        if (q.getMaxSalary() != null) parts.add("earning at most " + money(q.getMaxSalary()));
        return String.join(", ", parts);
    }

    private static String sortLabel(EmployeeQuery q) {
        boolean desc = q.getSortDirection() == EmployeeQuery.SortDirection.DESC;
        return switch (q.getSortBy()) {
            case HIRE_DATE -> desc ? "hire date (newest first)" : "hire date (oldest first)";
            case SALARY -> desc ? "salary (highest first)" : "salary (lowest first)";
            case NAME -> desc ? "name (Z-A)" : "name (A-Z)";
        };
    }

    private static String date(LocalDate d) {
        return d.format(DATE);
    }

    private static String money(BigDecimal amount) {
        NumberFormat f = NumberFormat.getCurrencyInstance(Locale.US);
        f.setMaximumFractionDigits(0);
        return f.format(amount);
    }
}
