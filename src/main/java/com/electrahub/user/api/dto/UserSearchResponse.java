package com.electrahub.user.api.dto;

import java.util.List;

public record UserSearchResponse(
        List<UserSummaryResponse> items,
        long total,
        int limit,
        int offset
) {
}
