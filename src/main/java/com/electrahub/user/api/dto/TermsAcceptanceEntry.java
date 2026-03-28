package com.electrahub.user.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TermsAcceptanceEntry(
        UUID termsVersionId,
        String version,
        OffsetDateTime acceptedAt,
        String devicePlatform,
        boolean linkedFromDevice
) {
}
