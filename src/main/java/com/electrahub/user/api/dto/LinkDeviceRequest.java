package com.electrahub.user.api.dto;

import jakarta.validation.constraints.NotBlank;

public record LinkDeviceRequest(
        @NotBlank String deviceId,
        @NotBlank String devicePlatform
) {
}
