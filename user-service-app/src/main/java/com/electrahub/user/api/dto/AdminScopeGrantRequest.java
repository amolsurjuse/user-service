package com.electrahub.user.api.dto;

import com.electrahub.user.domain.AdminScopeAccess;
import com.electrahub.user.domain.AdminScopeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminScopeGrantRequest(
        @NotNull AdminScopeType scopeType,
        @NotBlank @Size(max = 96) String scopeId,
        @NotNull AdminScopeAccess accessLevel
) {
}
