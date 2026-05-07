package com.electrahub.user.api;

import com.electrahub.user.api.dto.TermsDtos;
import com.electrahub.user.security.InternalApiKeyGuard;
import com.electrahub.user.service.TermsAudience;
import com.electrahub.user.service.TermsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/internal/terms")
public class InternalTermsController {

    private final InternalApiKeyGuard internalApiKeyGuard;
    private final TermsService termsService;

    public InternalTermsController(InternalApiKeyGuard internalApiKeyGuard, TermsService termsService) {
        this.internalApiKeyGuard = internalApiKeyGuard;
        this.termsService = termsService;
    }

    @GetMapping("/gate-status")
    public TermsDtos.TermsGateStatusResponse gateStatus(
            @RequestHeader(value = InternalApiKeyGuard.HEADER_NAME, required = false) String internalApiKey,
            @RequestParam UUID userId,
            @RequestParam(required = false) String uiType
    ) {
        internalApiKeyGuard.assertAuthorized(internalApiKey);
        return termsService.gateStatus(userId, TermsAudience.from(uiType));
    }
}
