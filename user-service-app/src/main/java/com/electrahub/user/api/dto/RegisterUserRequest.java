package com.electrahub.user.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record RegisterUserRequest(
        @Email @NotBlank String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank String firstName,
        @NotBlank String lastName,
        @Pattern(regexp = "^\\+?[1-9]\\d{7,14}$", message = "Invalid phone number") String phoneNumber,
        @Valid @NotNull AddressDto address
) {
}
