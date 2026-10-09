package com.employeehub.dto;

public class EmployeeMetricsDto {
    private long totalEmployees;
    private long activeEmployees;
    private long departments;
    private long recentHires;

    public EmployeeMetricsDto() {
    }

    public EmployeeMetricsDto(long totalEmployees, long activeEmployees, long departments, long recentHires) {
        this.totalEmployees = totalEmployees;
        this.activeEmployees = activeEmployees;
        this.departments = departments;
        this.recentHires = recentHires;
    }

    public long getTotalEmployees() {
        return totalEmployees;
    }

    public void setTotalEmployees(long totalEmployees) {
        this.totalEmployees = totalEmployees;
    }

    public long getActiveEmployees() {
        return activeEmployees;
    }

    public void setActiveEmployees(long activeEmployees) {
        this.activeEmployees = activeEmployees;
    }

    public long getDepartments() {
        return departments;
    }

    public void setDepartments(long departments) {
        this.departments = departments;
    }

    public long getRecentHires() {
        return recentHires;
    }

    public void setRecentHires(long recentHires) {
        this.recentHires = recentHires;
    }
}
