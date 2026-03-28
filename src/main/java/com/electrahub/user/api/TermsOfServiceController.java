package com.electrahub.user.api;

import com.electrahub.user.api.dto.AcceptTermsRequest;
import com.electrahub.user.api.dto.AcceptTermsResponse;
import com.electrahub.user.api.dto.AcceptanceStatusResponse;
import com.electrahub.user.api.dto.LinkDeviceRequest;
import com.electrahub.user.api.dto.LinkDeviceResponse;
import com.electrahub.user.api.dto.TermsHistoryResponse;
import com.electrahub.user.api.dto.TermsVersionResponse;
import com.electrahub.user.security.AuthenticatedUser;
import com.electrahub.user.service.TermsOfServiceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/terms")
public class TermsOfServiceController {
    private static final Logger LOGGER = LoggerFactory.getLogger(TermsOfServiceController.class);

    private final TermsOfServiceService termsService;

    public TermsOfServiceController(TermsOfServiceService termsService) {
        this.termsService = termsService;
    }

    @GetMapping("/current")
    public TermsVersionResponse getCurrentTerms() {
        return termsService.getCurrentTerms();
    }

    @PostMapping("/accept")
    @ResponseStatus(HttpStatus.CREATED)
    public AcceptTermsResponse acceptTerms(
            @Valid @RequestBody AcceptTermsRequest request,
            HttpServletRequest httpRequest) {
        UUID userId = resolveUserId();
        String ipAddress = httpRequest.getRemoteAddr();
        return termsService.acceptTerms(request, userId, ipAddress);
    }

    @GetMapping("/acceptance/status")
    public AcceptanceStatusResponse getAcceptanceStatus(
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false) String devicePlatform) {
        UUID userId = resolveUserId();
        return termsService.getAcceptanceStatus(deviceId, userId);
    }

    @PostMapping("/link-device")
    public LinkDeviceResponse linkDevice(@Valid @RequestBody LinkDeviceRequest request) {
        UUID userId = requireUserId();
        return termsService.linkDevice(request, userId);
    }

    @GetMapping("/history")
    public TermsHistoryResponse getHistory() {
        UUID userId = requireUserId();
        return termsService.getHistory(userId);
    }

    /**
     * Extracts user ID from security context if authenticated, otherwise returns null.
     * Used for endpoints that support both anonymous and authenticated access.
     */
    private UUID resolveUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return user.userId();
        }
        return null;
    }

    /**
     * Extracts user ID from security context or throws if not authenticated.
     * Used for endpoints that require authentication.
     */
    private UUID requireUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return user.userId();
        }
        throw new SecurityException("Authentication required");
    }
}
