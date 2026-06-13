package com.electrahub.user.security;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.electrahub.user.api.error.UnauthorizedException;
import com.electrahub.user.config.RbacSyncProperties;
import org.springframework.stereotype.Component;

@Component
public class InternalApiKeyGuard {
    private static final Logger LOGGER = LoggerFactory.getLogger(InternalApiKeyGuard.class);


    public static final String HEADER_NAME = "X-Internal-Api-Key";

    private final RbacSyncProperties rbacSyncProperties;

    /**
     * Executes internal api key guard for `InternalApiKeyGuard`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.security`.
     * @param rbacSyncProperties input consumed by InternalApiKeyGuard.
     */
    public InternalApiKeyGuard(RbacSyncProperties rbacSyncProperties) {
        LOGGER.info("CODEx_ENTRY_LOG: Entering InternalApiKeyGuard#InternalApiKeyGuard");
        LOGGER.debug("CODEx_ENTRY_LOG: Entering InternalApiKeyGuard#InternalApiKeyGuard with debug context");
        this.rbacSyncProperties = rbacSyncProperties;
    }

    /**
     * Executes assert authorized for `InternalApiKeyGuard`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.security`.
     * @param incomingApiKey input consumed by assertAuthorized.
     */
    public void assertAuthorized(String incomingApiKey) {
        String expected = rbacSyncProperties.getInternalApiKey();
        if (expected == null || expected.isBlank() || incomingApiKey == null || !expected.equals(incomingApiKey)) {
            throw new UnauthorizedException("Invalid internal API key");
        }
    }
}
