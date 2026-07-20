package com.electrahub.user.service;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AdminScopeCacheInvalidationListenerTest {

    @Test
    void invalidatesOnlyTheChangedAdministratorsScope() {
        GatewayRbacCacheInvalidationClient client = mock(GatewayRbacCacheInvalidationClient.class);
        AdminScopeCacheInvalidationListener listener = new AdminScopeCacheInvalidationListener(client);
        UUID userId = UUID.randomUUID();

        listener.invalidateAccessScope(new AdminScopeGrantsChangedEvent(userId));

        verify(client).invalidateAccessScope(userId);
    }
}
