package com.electrahub.user.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AcceptTermsResponse(
        UUID acceptanceId,
        boolean accepted,
        OffsetDateTime acceptedAt
) {
}
