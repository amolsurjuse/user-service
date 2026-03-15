package com.electrahub.user.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AuthenticateUserRequest(
        @Email @NotBlank String email,
        @NotBlank String password
) {
}
