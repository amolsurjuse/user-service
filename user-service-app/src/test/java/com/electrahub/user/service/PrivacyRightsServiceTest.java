package com.electrahub.user.service;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PrivacyRightsServiceTest {
    private final OffsetDateTime now = OffsetDateTime.parse("2026-08-12T12:00:00Z");

    @Test
    void requiresReviewBeforeTerminalDecision() {
        assertThatThrownBy(() -> PrivacyRightsService.validateTransition(
                "RECEIVED", "FULFILLED", null, null, now))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void lawfulHoldRequiresLegalBasisAndFutureExpiry() {
        assertThatThrownBy(() -> PrivacyRightsService.validateTransition(
                "IN_REVIEW", "LAWFUL_HOLD_APPLIED", "tax retention", now.minusDays(1), now))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(PrivacyRightsService.validateTransition("IN_REVIEW", "LAWFUL_HOLD_APPLIED",
                "tax retention", now.plusYears(7), now)).isEqualTo("IN_REVIEW");
    }

    @Test
    void terminalDecisionCannotBeRewritten() {
        assertThatThrownBy(() -> PrivacyRightsService.validateTransition(
                "FULFILLED", "REJECTED", null, null, now))
                .isInstanceOf(IllegalStateException.class);
    }
}
