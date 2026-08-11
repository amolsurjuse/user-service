package com.electrahub.user.security;

import com.electrahub.user.api.error.UnauthorizedException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InternalServiceTokenGuardTest {

    @Test
    void acceptsMatchingToken() {
        InternalServiceTokenGuard guard = new InternalServiceTokenGuard("shared-token");

        assertThatNoException().isThrownBy(() -> guard.assertAuthorized("shared-token"));
    }

    @Test
    void rejectsMissingMismatchedAndUnconfiguredTokens() {
        InternalServiceTokenGuard configured = new InternalServiceTokenGuard("shared-token");
        InternalServiceTokenGuard unconfigured = new InternalServiceTokenGuard("");

        assertThatThrownBy(() -> configured.assertAuthorized(null)).isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> configured.assertAuthorized("wrong-token")).isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> unconfigured.assertAuthorized("anything")).isInstanceOf(UnauthorizedException.class);
    }
}
