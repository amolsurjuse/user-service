package com.electrahub.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.rbac")
public class RbacSyncProperties {

    private String policyKey = "gateway-default";
    private String internalApiKey = "dev-rbac-internal-key";
    private String gatewayBaseUrl = "http://api-gateway:8090";
    private String gatewayInvalidatePath = "/internal/rbac/cache/invalidate";

    public String getPolicyKey() {
        return policyKey;
    }

    public void setPolicyKey(String policyKey) {
        this.policyKey = policyKey;
    }

    public String getInternalApiKey() {
        return internalApiKey;
    }

    public void setInternalApiKey(String internalApiKey) {
        this.internalApiKey = internalApiKey;
    }

    public String getGatewayBaseUrl() {
        return gatewayBaseUrl;
    }

    public void setGatewayBaseUrl(String gatewayBaseUrl) {
        this.gatewayBaseUrl = gatewayBaseUrl;
    }

    public String getGatewayInvalidatePath() {
        return gatewayInvalidatePath;
    }

    public void setGatewayInvalidatePath(String gatewayInvalidatePath) {
        this.gatewayInvalidatePath = gatewayInvalidatePath;
    }
}
