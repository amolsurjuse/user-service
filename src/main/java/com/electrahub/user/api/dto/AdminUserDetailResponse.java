package com.electrahub.user.api.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AdminUserDetailResponse(
        UUID userId,
        String email,
        String firstName,
        String lastName,
        String phoneNumber,
        String street,
        String city,
        String state,
        String postalCode,
        String countryCode,
        String countryName,
        String countryDialCode,
        boolean enabled,
        boolean pendingDeletion,
        OffsetDateTime deletionRequestedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<String> roles
) {
}
