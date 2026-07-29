package com.electrahub.user.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        AddressDto address,
        @Size(max = 200) String billingLegalName,
        @Size(max = 80) String taxRegistrationNumber
) {
    public UpdateUserProfileRequest(String firstName, String lastName, AddressDto address) {
        this(firstName, lastName, address, null, null);
    }
}
