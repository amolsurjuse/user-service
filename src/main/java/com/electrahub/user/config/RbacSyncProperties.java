package com.electrahub.user.config;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.rbac")
public class RbacSyncProperties {
    private static final Logger LOGGER = LoggerFactory.getLogger(RbacSyncProperties.class);


    private String policyKey = "gateway-default";
    private String internalApiKey = "dev-rbac-internal-key";
    private String gatewayBaseUrl = "http://api-gateway:8090";
    private String gatewayInvalidatePath = "/internal/rbac/cache/invalidate";

    /**
     * Retrieves get policy key for `RbacSyncProperties`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.config`.
     * @return result produced by getPolicyKey.
     */
    public String getPolicyKey() {
        LOGGER.info("CODEx_ENTRY_LOG: Entering RbacSyncProperties#getPolicyKey");
        LOGGER.debug("CODEx_ENTRY_LOG: Entering RbacSyncProperties#getPolicyKey with debug context");
        return policyKey;
    }

    /**
     * Updates set policy key for `RbacSyncProperties`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.config`.
     * @param policyKey input consumed by setPolicyKey.
     */
    public void setPolicyKey(String policyKey) {
        this.policyKey = policyKey;
    }

    /**
     * Retrieves get internal api key for `RbacSyncProperties`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.config`.
     * @return result produced by getInternalApiKey.
     */
    public String getInternalApiKey() {
        return internalApiKey;
    }

    /**
     * Updates set internal api key for `RbacSyncProperties`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.config`.
     * @param internalApiKey input consumed by setInternalApiKey.
     */
    public void setInternalApiKey(String internalApiKey) {
        this.internalApiKey = internalApiKey;
    }

    /**
     * Retrieves get gateway base url for `RbacSyncProperties`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.config`.
     * @return result produced by getGatewayBaseUrl.
     */
    public String getGatewayBaseUrl() {
        return gatewayBaseUrl;
    }

    /**
     * Updates set gateway base url for `RbacSyncProperties`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.config`.
     * @param gatewayBaseUrl input consumed by setGatewayBaseUrl.
     */
    public void setGatewayBaseUrl(String gatewayBaseUrl) {
        this.gatewayBaseUrl = gatewayBaseUrl;
    }

    /**
     * Retrieves get gateway invalidate path for `RbacSyncProperties`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.config`.
     * @return result produced by getGatewayInvalidatePath.
     */
    public String getGatewayInvalidatePath() {
        return gatewayInvalidatePath;
    }

    /**
     * Updates set gateway invalidate path for `RbacSyncProperties`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.config`.
     * @param gatewayInvalidatePath input consumed by setGatewayInvalidatePath.
     */
    public void setGatewayInvalidatePath(String gatewayInvalidatePath) {
        this.gatewayInvalidatePath = gatewayInvalidatePath;
    }
}
