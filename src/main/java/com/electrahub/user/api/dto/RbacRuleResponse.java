package com.electrahub.user.api.dto;

import java.util.List;
import java.util.UUID;

public record RbacRuleResponse(
        UUID ruleId,
        int sortOrder,
        String name,
        List<String> methods,
        String pathPattern,
        String effect,
        boolean allowAnonymous,
        List<String> requiredRoles
) {
}
