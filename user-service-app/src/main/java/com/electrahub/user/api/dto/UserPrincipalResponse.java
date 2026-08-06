package com.electrahub.user.api.dto;

import java.util.List;
import java.util.UUID;

public record UserPrincipalResponse(
        UUID userId,
        String email,
        boolean enabled,
        boolean emailVerified,
        List<String> roles,
        boolean pendingDeletion,
        String tenantId
) {
    public UserPrincipalResponse(UUID userId, String email, boolean enabled, boolean emailVerified,
                                 List<String> roles, boolean pendingDeletion) {
        this(userId, email, enabled, emailVerified, roles, pendingDeletion, "electrahub");
    }
}
