package com.electrahub.user.security;

import com.electrahub.user.api.error.UnauthorizedException;
import com.electrahub.user.config.RbacSyncProperties;
import org.springframework.stereotype.Component;

@Component
public class InternalApiKeyGuard {

    public static final String HEADER_NAME = "X-Internal-Api-Key";

    private final RbacSyncProperties rbacSyncProperties;

    public InternalApiKeyGuard(RbacSyncProperties rbacSyncProperties) {
        this.rbacSyncProperties = rbacSyncProperties;
    }

    public void assertAuthorized(String incomingApiKey) {
        String expected = rbacSyncProperties.getInternalApiKey();
        if (expected == null || expected.isBlank() || incomingApiKey == null || !expected.equals(incomingApiKey)) {
            throw new UnauthorizedException("Invalid internal API key");
        }
    }
}
