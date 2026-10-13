package com.employeehub.query;

import com.employeehub.dto.EmployeeDto;
import com.employeehub.dto.EmployeeSearchResult;
import com.employeehub.model.Department;
import com.employeehub.model.Employee;
import com.employeehub.repository.DepartmentRepository;
import com.employeehub.repository.EmployeeRepository;
import com.employeehub.service.EmployeeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Validator + Specifications + EmployeeService.search against H2. Uses its own departments
 * ("Query Sales", "Query Support") so seeded/other-test data doesn't affect counts; rolled back after each test.
 */
@SpringBootTest
@Transactional
class EmployeeQueryTest {

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private EmployeeQueryValidator validator;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    private final LocalDate today = LocalDate.now();

    @BeforeEach
    void seed() {
        Department sales = departmentRepository.save(new Department("Query Sales"));
        Department support = departmentRepository.save(new Department("Query Support"));
        save("Ana", "Lopez", "Account Executive", 90_000, today.minusDays(10), "Active", sales);
        save("Ben", "Ortiz", "Sales Manager", 130_000, today.minusDays(200), "Active", sales);
        save("Cara", "Singh", "Account Executive", 85_000, today.minusDays(400), "On Leave", sales);
        save("Dev", "Patel", "Support Engineer", 70_000, today.minusDays(40), "ACTIVE", support); // legacy status casing
    }

    private void save(String first, String last, String position, int salary, LocalDate hired, String status, Department dept) {
        employeeRepository.save(new Employee(first, last, (first + "." + last + "@query-test.com").toLowerCase(),
                position, BigDecimal.valueOf(salary), hired, status, dept));
    }

    private EmployeeSearchResult run(EmployeeQuery q, boolean canViewSalary) {
        validator.validate(q, canViewSalary);
        return employeeService.search(q);
    }

    private EmployeeQuery inSales() {
        EmployeeQuery q = new EmployeeQuery();
        q.setDepartment("query sales"); // different casing on purpose
        return q;
    }

    @Test
    void countsByDepartmentCaseInsensitively() {
        EmployeeQuery q = inSales();
        q.setResultType(EmployeeQuery.ResultType.COUNT);

        EmployeeSearchResult result = run(q, false);

        assertThat(result.totalMatches()).isEqualTo(3);
        assertThat(result.employees()).isEmpty();
        assertThat(q.getDepartment()).isEqualTo("Query Sales"); // normalized to the real name
    }

    @Test
    void filtersByHireDateRangeAndSortsNewestFirst() {
        EmployeeQuery q = inSales();
        q.setHiredOnOrAfter(today.minusDays(365));
        q.setSortBy(EmployeeQuery.SortField.HIRE_DATE);
        q.setSortDirection(EmployeeQuery.SortDirection.DESC);

        EmployeeSearchResult result = run(q, false);

        assertThat(result.employees()).extracting(EmployeeDto::getFirstName).containsExactly("Ana", "Ben");
    }

    @Test
    void statusMatchesLegacyCasing() {
        EmployeeQuery q = new EmployeeQuery();
        q.setDepartment("Query Support");
        q.setStatus("active");

        assertThat(run(q, false).totalMatches()).isEqualTo(1);
    }

    @Test
    void positionAndNameSearchArePartialAndCaseInsensitive() {
        EmployeeQuery byPosition = inSales();
        byPosition.setPositionContains("ACCOUNT exec");
        assertThat(run(byPosition, false).totalMatches()).isEqualTo(2);

        EmployeeQuery byFullName = new EmployeeQuery();
        byFullName.setNameContains("ben ort");
        assertThat(run(byFullName, false).employees()).extracting(EmployeeDto::getLastName).containsExactly("Ortiz");
    }

    @Test
    void likeWildcardsInSearchTextAreMatchedLiterally() {
        EmployeeQuery q = new EmployeeQuery();
        q.setNameContains("%");
        assertThat(run(q, false).totalMatches()).isZero();
    }

    @Test
    void salaryFilterWorksForManagersAndIsRejectedForReadOnlyUsers() {
        EmployeeQuery forManager = inSales();
        forManager.setMinSalary(BigDecimal.valueOf(88_000));
        assertThat(run(forManager, true).totalMatches()).isEqualTo(2);

        EmployeeQuery forViewer = inSales();
        forViewer.setMinSalary(BigDecimal.valueOf(88_000));
        assertThatThrownBy(() -> validator.validate(forViewer, false))
                .isInstanceOf(InvalidQueryException.class)
                .hasMessageContaining("admins and managers");

        EmployeeQuery sortForViewer = inSales();
        sortForViewer.setSortBy(EmployeeQuery.SortField.SALARY);
        assertThatThrownBy(() -> validator.validate(sortForViewer, false)).isInstanceOf(InvalidQueryException.class);
    }

    @Test
    void limitIsCappedButTotalStillCountsEverything() {
        EmployeeQuery q = inSales();
        q.setLimit(1);
        EmployeeSearchResult result = run(q, false);
        assertThat(result.totalMatches()).isEqualTo(3);
        assertThat(result.employees()).hasSize(1);

        EmployeeQuery huge = new EmployeeQuery();
        huge.setLimit(10_000);
        validator.validate(huge, false);
        assertThat(huge.getLimit()).isEqualTo(EmployeeQuery.MAX_LIMIT);
    }

    @Test
    void rejectsUnknownDepartmentUnknownStatusAndBackwardsRanges() {
        EmployeeQuery unknownDept = new EmployeeQuery();
        unknownDept.setDepartment("Sails");
        assertThatThrownBy(() -> validator.validate(unknownDept, true)).hasMessageContaining("no department called \"Sails\"");

        EmployeeQuery unknownStatus = new EmployeeQuery();
        unknownStatus.setStatus("Retired");
        assertThatThrownBy(() -> validator.validate(unknownStatus, true)).hasMessageContaining("Unknown status");

        EmployeeQuery backwardsDates = new EmployeeQuery();
        backwardsDates.setHiredOnOrAfter(today);
        backwardsDates.setHiredOnOrBefore(today.minusDays(1));
        assertThatThrownBy(() -> validator.validate(backwardsDates, true)).isInstanceOf(InvalidQueryException.class);

        EmployeeQuery backwardsSalary = new EmployeeQuery();
        backwardsSalary.setMinSalary(BigDecimal.TEN);
        backwardsSalary.setMaxSalary(BigDecimal.ONE);
        assertThatThrownBy(() -> validator.validate(backwardsSalary, true)).isInstanceOf(InvalidQueryException.class);
    }
}
