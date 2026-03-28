package com.electrahub.user.api.dto;

import java.util.List;

public record TermsHistoryResponse(
        List<TermsAcceptanceEntry> acceptances
) {
}
