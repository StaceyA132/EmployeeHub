package com.employeehub;

import com.employeehub.model.AppUser;
import com.employeehub.model.Department;
import com.employeehub.model.Employee;
import com.employeehub.model.Role;
import com.employeehub.repository.DepartmentRepository;
import com.employeehub.repository.EmployeeRepository;
import com.employeehub.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner loadInitialData(UserRepository userRepository, DepartmentRepository departmentRepository, EmployeeRepository employeeRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.count() == 0) {
                AppUser admin = new AppUser();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setRoles(Set.of(Role.ROLE_ADMIN));
                userRepository.save(admin);

                AppUser manager = new AppUser();
                manager.setUsername("manager");
                manager.setPassword(passwordEncoder.encode("manager123"));
                manager.setRoles(Set.of(Role.ROLE_MANAGER));
                userRepository.save(manager);
            }

            if (departmentRepository.count() == 0) {
                Department engineering = departmentRepository.save(new Department("Engineering"));
                Department hr = departmentRepository.save(new Department("Human Resources"));

                employeeRepository.save(new Employee("Stacey", "Adams", "stacey.adams@example.com", "Engineering Manager", new BigDecimal("120000"), LocalDate.now().minusYears(3), "Active", engineering));
                employeeRepository.save(new Employee("James", "Lee", "james.lee@example.com", "HR Specialist", new BigDecimal("80000"), LocalDate.now().minusYears(1), "Active", hr));
            }
        };
    }
}
