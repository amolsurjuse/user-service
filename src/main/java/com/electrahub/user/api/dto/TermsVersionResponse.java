package com.electrahub.user.api.dto;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record TermsVersionResponse(
        UUID id,
        String version,
        String title,
        String contentUrl,
        String summary,
        OffsetDateTime effectiveDate,
        String enforcementMode,
        Map<String, Object> metadata
) {
}
