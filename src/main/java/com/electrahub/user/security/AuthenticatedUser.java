package com.electrahub.user.security;

import java.util.List;
import java.util.UUID;

public record AuthenticatedUser(
        UUID userId,
        String email,
        List<String> roles
) {
    public boolean hasRole(String role) {
        return roles != null && roles.stream().anyMatch(role::equalsIgnoreCase);
    }
}
