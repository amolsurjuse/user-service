package com.electrahub.user.api.dto;

import java.util.List;

public record GatewayRbacPolicyResponse(
        String policyKey,
        String roleHierarchy,
        String defaultDecision,
        long version,
        List<GatewayRbacRuleResponse> rules
) {
}
