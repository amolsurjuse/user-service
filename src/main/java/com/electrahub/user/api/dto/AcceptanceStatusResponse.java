package com.electrahub.user.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AcceptanceStatusResponse(
        UUID termsVersionId,
        boolean accepted,
        OffsetDateTime acceptedAt,
        String currentVersion,
        String acceptedVersion,
        boolean requiresAcceptance
) {
}
