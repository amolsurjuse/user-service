package com.electrahub.user.service;

import com.electrahub.user.config.RbacSyncProperties;
import com.electrahub.user.security.InternalApiKeyGuard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class GatewayRbacCacheInvalidationClient {

    private static final Logger log = LoggerFactory.getLogger(GatewayRbacCacheInvalidationClient.class);

    private final RestClient restClient;
    private final RbacSyncProperties rbacSyncProperties;

    /**
     * Executes gateway rbac cache invalidation client for `GatewayRbacCacheInvalidationClient`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.service`.
     * @param rbacSyncProperties input consumed by GatewayRbacCacheInvalidationClient.
     */
    public GatewayRbacCacheInvalidationClient(RbacSyncProperties rbacSyncProperties) {
        log.info("CODEx_ENTRY_LOG: Entering GatewayRbacCacheInvalidationClient#GatewayRbacCacheInvalidationClient");
        log.debug("CODEx_ENTRY_LOG: Entering GatewayRbacCacheInvalidationClient#GatewayRbacCacheInvalidationClient with debug context");
        this.rbacSyncProperties = rbacSyncProperties;
        this.restClient = RestClient.builder()
                .baseUrl(rbacSyncProperties.getGatewayBaseUrl())
                .build();
    }

    /**
     * Executes invalidate for `GatewayRbacCacheInvalidationClient`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.service`.
     */
    public void invalidate() {
        try {
            restClient.post()
                    .uri(rbacSyncProperties.getGatewayInvalidatePath())
                    .header(InternalApiKeyGuard.HEADER_NAME, rbacSyncProperties.getInternalApiKey())
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            HttpStatusCode statusCode = ex.getStatusCode();
            log.warn("Gateway RBAC cache invalidation failed: status={} body={}",
                    statusCode == null ? "unknown" : statusCode.value(),
                    ex.getResponseBodyAsString());
        } catch (Exception ex) {
            log.warn("Gateway RBAC cache invalidation failed: {}", ex.getMessage());
        }
    }
}
