package com.electrahub.user.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateUserProfileRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        AddressDto address
) {
}
