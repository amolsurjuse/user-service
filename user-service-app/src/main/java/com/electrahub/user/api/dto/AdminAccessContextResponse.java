package com.electrahub.user.api.dto;

import java.util.List;
import java.util.UUID;

public record AdminAccessContextResponse(
        UUID actorId,
        boolean systemAdmin,
        List<AdminScopeGrantResponse> grants
) {
}
