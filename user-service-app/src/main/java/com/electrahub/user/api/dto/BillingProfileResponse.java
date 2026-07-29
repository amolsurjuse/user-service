package com.electrahub.user.api.dto;

import java.util.UUID;

public record BillingProfileResponse(
        UUID userId,
        String email,
        String firstName,
        String lastName,
        String street,
        String city,
        String state,
        String postalCode,
        String countryCode,
        String billingLegalName,
        String taxRegistrationNumber
) {
}
