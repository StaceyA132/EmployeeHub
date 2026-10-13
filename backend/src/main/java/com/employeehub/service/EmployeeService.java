package com.employeehub.service;

import com.employeehub.dto.EmployeeDto;
import com.employeehub.dto.EmployeeMetricsDto;
import com.employeehub.dto.EmployeeSearchResult;
import com.employeehub.query.EmployeeQuery;

import java.util.List;

public interface EmployeeService {
    List<EmployeeDto> getAllEmployees();
    EmployeeDto getEmployeeById(Long id);
    EmployeeDto createEmployee(EmployeeDto dto);
    EmployeeDto updateEmployee(Long id, EmployeeDto dto);
    void deleteEmployee(Long id);
    EmployeeMetricsDto getEmployeeMetrics();
    /** Runs an already-validated query (see EmployeeQueryValidator). */
    EmployeeSearchResult search(EmployeeQuery query);
}
