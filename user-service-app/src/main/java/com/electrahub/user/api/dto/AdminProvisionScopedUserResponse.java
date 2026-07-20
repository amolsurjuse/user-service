package com.electrahub.user.api.dto;

import java.util.List;
import java.util.UUID;

public record AdminProvisionScopedUserResponse(
        UUID userId,
        String email,
        List<AdminScopeGrantResponse> grants
) {
}
