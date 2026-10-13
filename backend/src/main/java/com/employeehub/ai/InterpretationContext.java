package com.employeehub.ai;

import java.time.LocalDate;
import java.util.List;

/**
 * Non-sensitive facts the AI needs to interpret a question: today's date (for "last 3 months") and the
 * department names (to map "sales team" to "Sales"). Never contains employee records.
 */
public record InterpretationContext(LocalDate today, List<String> departments, boolean canViewSalary) {
}
