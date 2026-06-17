package com.electrahub.user.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetUserPasswordRequest(
        @NotBlank
        @Size(min = 8, max = 128)
        String newPassword
) {
}
