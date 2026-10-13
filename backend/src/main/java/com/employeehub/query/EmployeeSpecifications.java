package com.employeehub.query;

import com.employeehub.model.Department;
import com.employeehub.model.Employee;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

/**
 * Turns a validated {@link EmployeeQuery} into a JPA Specification. Values are always bound as
 * query parameters by JPA, never concatenated into SQL.
 */
public final class EmployeeSpecifications {

    private EmployeeSpecifications() {
    }

    public static Specification<Employee> matching(EmployeeQuery q) {
        Specification<Employee> spec = Specification.where(null);
        if (q.getDepartment() != null) {
            spec = spec.and(departmentIs(q.getDepartment()));
        }
        if (q.getStatus() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("status")), q.getStatus().toLowerCase()));
        }
        if (q.getPositionContains() != null) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("position")), contains(q.getPositionContains()), '\\'));
        }
        if (q.getNameContains() != null) {
            String pattern = contains(q.getNameContains());
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("firstName")), pattern, '\\'),
                    cb.like(cb.lower(root.get("lastName")), pattern, '\\'),
                    cb.like(cb.lower(cb.concat(cb.concat(root.get("firstName"), " "), root.get("lastName"))), pattern, '\\')));
        }
        if (q.getHiredOnOrAfter() != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("hireDate"), q.getHiredOnOrAfter()));
        }
        if (q.getHiredOnOrBefore() != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("hireDate"), q.getHiredOnOrBefore()));
        }
        if (q.getMinSalary() != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("salary"), q.getMinSalary()));
        }
        if (q.getMaxSalary() != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("salary"), q.getMaxSalary()));
        }
        return spec;
    }

    private static Specification<Employee> departmentIs(String name) {
        return (root, query, cb) -> {
            Join<Employee, Department> department = root.join("department", JoinType.INNER);
            return cb.equal(cb.lower(department.get("name")), name.toLowerCase());
        };
    }

    // Escape LIKE wildcards so "%" or "_" in the text are matched literally
    private static String contains(String text) {
        String escaped = text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
