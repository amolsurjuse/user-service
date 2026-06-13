package com.electrahub.user.api.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record RbacPolicyResponse(
        String policyKey,
        String roleHierarchy,
        String defaultDecision,
        long version,
        OffsetDateTime updatedAt,
        List<String> availableRoles,
        List<RbacRuleResponse> rules
) {
}
