package com.electrahub.user.security;

import java.util.List;
import java.util.UUID;

public record AuthenticatedUser(
        UUID userId,
        String email,
        List<String> roles
) {
    /**
     * Executes has role for `AuthenticatedUser`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.security`.
     * @param role input consumed by hasRole.
     * @return result produced by hasRole.
     */
    public boolean hasRole(String role) {
        return roles != null && roles.stream().anyMatch(role::equalsIgnoreCase);
    }
}
