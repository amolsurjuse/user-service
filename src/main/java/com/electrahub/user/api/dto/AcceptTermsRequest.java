package com.electrahub.user.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AcceptTermsRequest(
        @NotNull UUID termsVersionId,
        String deviceId,
        String devicePlatform,
        String deviceModel,
        String appVersion
) {
}
