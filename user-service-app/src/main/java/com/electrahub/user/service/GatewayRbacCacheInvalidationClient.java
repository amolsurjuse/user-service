package com.electrahub.user.service;

import com.electrahub.user.config.RbacSyncProperties;
import com.electrahub.user.security.InternalApiKeyGuard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.UUID;

@Component
public class GatewayRbacCacheInvalidationClient {

    private static final Logger log = LoggerFactory.getLogger(GatewayRbacCacheInvalidationClient.class);
    private static final DefaultRedisScript<Long> INVALIDATE_ACCESS_SCOPE_SCRIPT = new DefaultRedisScript<>("""
            local version = redis.call('INCR', KEYS[1])
            redis.call('PEXPIRE', KEYS[1], ARGV[1])
            local scopeKeys = redis.call('SMEMBERS', KEYS[2])
            if #scopeKeys > 0 then
                redis.call('DEL', unpack(scopeKeys))
            end
            redis.call('DEL', KEYS[2])
            return version
            """, Long.class);

    private final RestClient restClient;
    private final RbacSyncProperties rbacSyncProperties;
    private final StringRedisTemplate redis;

    /**
     * Executes gateway rbac cache invalidation client for `GatewayRbacCacheInvalidationClient`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.service`.
     * @param rbacSyncProperties input consumed by GatewayRbacCacheInvalidationClient.
     */
    public GatewayRbacCacheInvalidationClient(
            RbacSyncProperties rbacSyncProperties,
            StringRedisTemplate redis
    ) {
        log.info("Entering GatewayRbacCacheInvalidationClient#GatewayRbacCacheInvalidationClient");
        log.debug("Entering GatewayRbacCacheInvalidationClient#GatewayRbacCacheInvalidationClient with debug context");
        this.rbacSyncProperties = rbacSyncProperties;
        this.redis = redis;
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
        invalidate(null);
    }

    public void invalidateAccessScope(UUID userId) {
        invalidate(userId);
    }

    private void invalidate(UUID userId) {
        if (userId != null && invalidateAccessScopeInRedis(userId)) {
            return;
        }
        try {
            restClient.post()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder.path(rbacSyncProperties.getGatewayInvalidatePath());
                        if (userId != null) {
                            builder.queryParam("userId", userId);
                        }
                        return builder.build();
                    })
                    .header(InternalApiKeyGuard.HEADER_NAME, rbacSyncProperties.getInternalApiKey())
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            HttpStatusCode statusCode = ex.getStatusCode();
            log.warn("Gateway RBAC cache invalidation failed for userId={}: status={} body={}",
                    userId,
                    statusCode == null ? "unknown" : statusCode.value(),
                    ex.getResponseBodyAsString());
        } catch (Exception ex) {
            log.warn("Gateway RBAC cache invalidation failed for userId={}: {}", userId, ex.getMessage());
        }
    }

    private boolean invalidateAccessScopeInRedis(UUID userId) {
        try {
            String prefix = normalizePrefix(rbacSyncProperties.getAccessScopeCachePrefix());
            redis.execute(
                    INVALIDATE_ACCESS_SCOPE_SCRIPT,
                    java.util.List.of(
                            prefix + "generation:" + userId,
                            prefix + "user:" + userId + ":keys"
                    ),
                    Long.toString(rbacSyncProperties.getAccessScopeCacheGenerationTtl().toMillis())
            );
            return true;
        } catch (Exception ex) {
            log.warn("Direct Redis access scope invalidation failed for userId={}; falling back to gateway: {}",
                    userId, ex.getMessage());
            return false;
        }
    }

    private String normalizePrefix(String configuredPrefix) {
        String value = configuredPrefix == null ? "" : configuredPrefix.trim();
        if (value.isEmpty()) {
            return "admin:access-scope:v1:";
        }
        return value.endsWith(":") ? value : value + ":";
    }
}
