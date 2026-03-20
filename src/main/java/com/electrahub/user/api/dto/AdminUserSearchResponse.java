package com.electrahub.user.api.dto;

import java.util.List;

public record AdminUserSearchResponse(
        List<AdminUserSummaryResponse> items,
        long total,
        int limit,
        int offset,
        int currentPage,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious
) {
}
