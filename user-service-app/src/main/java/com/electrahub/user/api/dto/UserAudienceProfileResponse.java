package com.electrahub.user.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserAudienceProfileResponse(
        UUID userId,
        String tenantId,
        String countryCode,
        OffsetDateTime registeredAt
) {
}
