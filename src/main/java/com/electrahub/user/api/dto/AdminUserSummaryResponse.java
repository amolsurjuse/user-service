package com.electrahub.user.api.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AdminUserSummaryResponse(
        UUID userId,
        String email,
        String firstName,
        String lastName,
        String phoneNumber,
        boolean enabled,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<String> roles
) {
}
