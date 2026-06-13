package com.electrahub.user.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminUpdateUserRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        String phoneNumber,
        boolean enabled,
        @Valid @NotNull AddressDto address
) {
}
