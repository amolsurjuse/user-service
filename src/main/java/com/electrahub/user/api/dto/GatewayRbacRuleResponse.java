package com.electrahub.user.api.dto;

import java.util.List;

public record GatewayRbacRuleResponse(
        String name,
        List<String> methods,
        String pathPattern,
        String effect,
        boolean allowAnonymous,
        List<String> requiredRoles
) {
}
