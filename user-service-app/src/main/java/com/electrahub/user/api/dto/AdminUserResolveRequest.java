package com.electrahub.user.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record AdminUserResolveRequest(
        @NotNull @Size(max = 500) List<@NotNull UUID> userIds
) {
}
