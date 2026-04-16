package com.electrahub.user.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AccountDeletionResponse(
        AccountDeletionDecision decision,
        String message,
        BigDecimal walletBalance,
        boolean activeCharging,
        boolean pendingDeletion,
        boolean deleted,
        boolean directDeletionEligible,
        OffsetDateTime deletionRequestedAt
) {
}

