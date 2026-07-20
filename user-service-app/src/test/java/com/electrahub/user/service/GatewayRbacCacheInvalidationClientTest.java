package com.electrahub.user.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.electrahub.user.config.RbacSyncProperties;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

@ExtendWith(MockitoExtension.class)
class GatewayRbacCacheInvalidationClientTest {

    @Mock
    private StringRedisTemplate redis;

    @Test
    void invalidatesEveryCachedScopeForTheChangedAdministratorDirectlyInRedis() {
        RbacSyncProperties properties = new RbacSyncProperties();
        properties.setAccessScopeCachePrefix("test:admin-scope");
        properties.setAccessScopeCacheGenerationTtl(Duration.ofMinutes(15));
        UUID administratorId = UUID.randomUUID();

        GatewayRbacCacheInvalidationClient client = new GatewayRbacCacheInvalidationClient(properties, redis);

        client.invalidateAccessScope(administratorId);

        verify(redis).execute(
                any(DefaultRedisScript.class),
                eq(List.of(
                        "test:admin-scope:generation:" + administratorId,
                        "test:admin-scope:user:" + administratorId + ":keys"
                )),
                eq("900000")
        );
    }
}
