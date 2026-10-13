package com.employeehub.security;

import org.springframework.security.core.Authentication;

/** Shared permission checks so every endpoint agrees on who can see what. */
public final class SecurityRoles {

    private SecurityRoles() {
    }

    /** Salaries are only visible to admins and managers. */
    public static boolean canViewSalary(Authentication auth) {
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_MANAGER"));
    }
}
