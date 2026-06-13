package com.electrahub.user.api.dto;

import java.util.List;
import java.util.UUID;

public record UserPrincipalResponse(
        UUID userId,
        String email,
        boolean enabled,
        List<String> roles
) {
}
