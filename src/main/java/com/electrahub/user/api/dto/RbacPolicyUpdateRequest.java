package com.electrahub.user.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record RbacPolicyUpdateRequest(
        @NotBlank String roleHierarchy,
        @NotBlank String defaultDecision,
        @Valid @NotEmpty List<RbacRuleRequest> rules
) {
}
