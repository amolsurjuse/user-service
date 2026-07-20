package com.electrahub.user.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Runs only after the grant transaction commits so the gateway cannot rebuild a scope from the
 * previous database state while invalidation is in progress.
 */
@Component
public class AdminScopeCacheInvalidationListener {

    private final GatewayRbacCacheInvalidationClient gatewayCacheInvalidationClient;

    public AdminScopeCacheInvalidationListener(GatewayRbacCacheInvalidationClient gatewayCacheInvalidationClient) {
        this.gatewayCacheInvalidationClient = gatewayCacheInvalidationClient;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void invalidateAccessScope(AdminScopeGrantsChangedEvent event) {
        gatewayCacheInvalidationClient.invalidateAccessScope(event.userId());
    }
}
