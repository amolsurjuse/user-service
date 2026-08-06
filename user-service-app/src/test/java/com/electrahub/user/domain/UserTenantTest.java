package com.electrahub.user.domain;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTenantTest {
    @Test
    void defaultsLegacyUsersAndNormalizesExplicitTenant() {
        User user = new User(UUID.randomUUID(), "driver@example.com", "hash", true, OffsetDateTime.now());
        assertThat(user.getTenantId()).isEqualTo("electrahub");

        user.setTenantId(" ACME-US ");
        assertThat(user.getTenantId()).isEqualTo("acme-us");
    }

    @Test
    void rejectsUnsafeTenantIdentifiers() {
        User user = new User(UUID.randomUUID(), "driver@example.com", "hash", true, OffsetDateTime.now());
        assertThatThrownBy(() -> user.setTenantId("../../other-tenant"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
