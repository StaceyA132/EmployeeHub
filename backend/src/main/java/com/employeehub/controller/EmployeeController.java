package com.employeehub.controller;

import com.employeehub.dto.EmployeeDto;
import com.employeehub.dto.EmployeeMetricsDto;
import com.employeehub.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    public ResponseEntity<List<EmployeeDto>> getAll() {
        return ResponseEntity.ok(employeeService.getAllEmployees());
    }

    @GetMapping("/metrics")
    public ResponseEntity<EmployeeMetricsDto> getMetrics() {
        return ResponseEntity.ok(employeeService.getEmployeeMetrics());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getEmployeeById(id));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_MANAGER')")
    @PostMapping
    public ResponseEntity<EmployeeDto> create(@Valid @RequestBody EmployeeDto dto) {
        return ResponseEntity.ok(employeeService.createEmployee(dto));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_MANAGER')")
    @PutMapping("/{id}")
    public ResponseEntity<EmployeeDto> update(@PathVariable Long id, @Valid @RequestBody EmployeeDto dto) {
        return ResponseEntity.ok(employeeService.updateEmployee(id, dto));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_MANAGER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.noContent().build();
    }
}
