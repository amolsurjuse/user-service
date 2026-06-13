package com.electrahub.user.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record RbacRuleRequest(
        @NotBlank String name,
        @NotEmpty List<@NotBlank String> methods,
        @NotBlank String pathPattern,
        @NotBlank String effect,
        boolean allowAnonymous,
        List<String> requiredRoles
) {
}
