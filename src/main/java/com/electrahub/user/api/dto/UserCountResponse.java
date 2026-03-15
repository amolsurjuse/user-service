package com.electrahub.user.api.dto;

public record UserCountResponse(
        String query,
        long count
) {
}
