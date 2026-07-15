package com.electrahub.user.api.dto;

import java.util.List;
import java.util.UUID;

public record AdminUserDirectoryEntryResponse(
        UUID userId,
        String email,
        String firstName,
        String lastName,
        List<String> roles
) {
}
