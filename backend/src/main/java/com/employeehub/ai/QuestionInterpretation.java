package com.employeehub.ai;

import com.employeehub.query.EmployeeQuery;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * What we ask the AI to return for a question. Text values (dates, enums) stay as strings here and are
 * parsed by {@link #toEmployeeQuery()}, so malformed model output becomes a clear INVALID_RESPONSE error.
 */
public class QuestionInterpretation {

    private boolean answerable;
    private String reason;
    private String resultType;
    private String department;
    private String status;
    private String positionContains;
    private String nameContains;
    private String hiredOnOrAfter;
    private String hiredOnOrBefore;
    private BigDecimal minSalary;
    private BigDecimal maxSalary;
    private String sortBy;
    private String sortDirection;
    private Integer limit;

    /** Converts to a query. Still needs EmployeeQueryValidator before it is run. */
    public EmployeeQuery toEmployeeQuery() {
        EmployeeQuery q = new EmployeeQuery();
        q.setDepartment(department);
        q.setStatus(status);
        q.setPositionContains(positionContains);
        q.setNameContains(nameContains);
        q.setHiredOnOrAfter(parseDate("hiredOnOrAfter", hiredOnOrAfter));
        q.setHiredOnOrBefore(parseDate("hiredOnOrBefore", hiredOnOrBefore));
        q.setMinSalary(minSalary);
        q.setMaxSalary(maxSalary);
        q.setResultType(parseEnum(EmployeeQuery.ResultType.class, "resultType", resultType));
        q.setSortBy(parseEnum(EmployeeQuery.SortField.class, "sortBy", sortBy));
        q.setSortDirection(parseEnum(EmployeeQuery.SortDirection.class, "sortDirection", sortDirection));
        q.setLimit(limit);
        return q;
    }

    private static LocalDate parseDate(String field, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new AiServiceException(AiServiceException.Kind.INVALID_RESPONSE,
                    "AI returned an invalid date for " + field + ": " + value, e);
        }
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String field, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AiServiceException(AiServiceException.Kind.INVALID_RESPONSE,
                    "AI returned an invalid value for " + field + ": " + value, e);
        }
    }

    public boolean isAnswerable() {
        return answerable;
    }

    public void setAnswerable(boolean answerable) {
        this.answerable = answerable;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getResultType() {
        return resultType;
    }

    public void setResultType(String resultType) {
        this.resultType = resultType;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPositionContains() {
        return positionContains;
    }

    public void setPositionContains(String positionContains) {
        this.positionContains = positionContains;
    }

    public String getNameContains() {
        return nameContains;
    }

    public void setNameContains(String nameContains) {
        this.nameContains = nameContains;
    }

    public String getHiredOnOrAfter() {
        return hiredOnOrAfter;
    }

    public void setHiredOnOrAfter(String hiredOnOrAfter) {
        this.hiredOnOrAfter = hiredOnOrAfter;
    }

    public String getHiredOnOrBefore() {
        return hiredOnOrBefore;
    }

    public void setHiredOnOrBefore(String hiredOnOrBefore) {
        this.hiredOnOrBefore = hiredOnOrBefore;
    }

    public BigDecimal getMinSalary() {
        return minSalary;
    }

    public void setMinSalary(BigDecimal minSalary) {
        this.minSalary = minSalary;
    }

    public BigDecimal getMaxSalary() {
        return maxSalary;
    }

    public void setMaxSalary(BigDecimal maxSalary) {
        this.maxSalary = maxSalary;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortDirection() {
        return sortDirection;
    }

    public void setSortDirection(String sortDirection) {
        this.sortDirection = sortDirection;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }
}
