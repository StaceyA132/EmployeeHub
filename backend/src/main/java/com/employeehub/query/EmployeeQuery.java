package com.employeehub.query;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A structured description of which employees to look up. Every filter is optional (null = no filter).
 * Built from an AI interpretation of a question, but always checked by {@link EmployeeQueryValidator}
 * before it reaches the database.
 */
public class EmployeeQuery {

    public enum ResultType { LIST, COUNT }

    public enum SortField { NAME, HIRE_DATE, SALARY }

    public enum SortDirection { ASC, DESC }

    public static final int DEFAULT_LIMIT = 20;
    public static final int MAX_LIMIT = 50;

    private String department;
    private String status;
    private String positionContains;
    private String nameContains;
    private LocalDate hiredOnOrAfter;
    private LocalDate hiredOnOrBefore;
    private BigDecimal minSalary;
    private BigDecimal maxSalary;
    private ResultType resultType = ResultType.LIST;
    private SortField sortBy = SortField.NAME;
    private SortDirection sortDirection = SortDirection.ASC;
    private Integer limit = DEFAULT_LIMIT;

    public boolean usesSalary() {
        return minSalary != null || maxSalary != null || sortBy == SortField.SALARY;
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

    public LocalDate getHiredOnOrAfter() {
        return hiredOnOrAfter;
    }

    public void setHiredOnOrAfter(LocalDate hiredOnOrAfter) {
        this.hiredOnOrAfter = hiredOnOrAfter;
    }

    public LocalDate getHiredOnOrBefore() {
        return hiredOnOrBefore;
    }

    public void setHiredOnOrBefore(LocalDate hiredOnOrBefore) {
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

    public ResultType getResultType() {
        return resultType;
    }

    public void setResultType(ResultType resultType) {
        this.resultType = resultType;
    }

    public SortField getSortBy() {
        return sortBy;
    }

    public void setSortBy(SortField sortBy) {
        this.sortBy = sortBy;
    }

    public SortDirection getSortDirection() {
        return sortDirection;
    }

    public void setSortDirection(SortDirection sortDirection) {
        this.sortDirection = sortDirection;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }
}
