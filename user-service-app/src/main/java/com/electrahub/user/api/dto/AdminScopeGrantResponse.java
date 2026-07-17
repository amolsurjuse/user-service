package com.electrahub.user.api.dto;

import com.electrahub.user.domain.AdminScopeAccess;
import com.electrahub.user.domain.AdminScopeType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminScopeGrantResponse(
        UUID grantId,
        AdminScopeType scopeType,
        String scopeId,
        AdminScopeAccess accessLevel,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
