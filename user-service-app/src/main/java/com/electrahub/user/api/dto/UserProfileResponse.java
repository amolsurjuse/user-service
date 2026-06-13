package com.electrahub.user.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserProfileResponse(
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
        OffsetDateTime createdAt
) {
}
