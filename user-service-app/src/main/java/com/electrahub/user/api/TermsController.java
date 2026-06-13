package com.electrahub.user.api;

import com.electrahub.user.api.dto.TermsDtos;
import com.electrahub.user.security.AuthenticatedUser;
import com.electrahub.user.service.TermsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@Validated
@RequestMapping("/api/v1/terms")
public class TermsController {

    private final TermsService termsService;

    public TermsController(TermsService termsService) {
        this.termsService = termsService;
    }

    @GetMapping("/current")
    public TermsDtos.TermsVersionResponse current() {
        return termsService.currentTerms();
    }

    @GetMapping("/status")
    public TermsDtos.TermsStatusResponse status(@AuthenticationPrincipal AuthenticatedUser user) {
        return termsService.status(requireUser(user).userId());
    }

    @PostMapping("/accept")
    @ResponseStatus(HttpStatus.CREATED)
    public TermsDtos.TermsAcceptResponse accept(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody TermsDtos.TermsAcceptRequest request,
            @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent,
            HttpServletRequest servletRequest
    ) {
        return termsService.acceptCurrent(requireUser(user).userId(), request, resolveIpAddress(servletRequest), userAgent);
    }

    @GetMapping("/history")
    public List<TermsDtos.TermsAcceptanceResponse> history(@AuthenticationPrincipal AuthenticatedUser user) {
        return termsService.history(requireUser(user).userId());
    }

    @GetMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public List<TermsDtos.AdminTermsVersionResponse> listVersions() {
        return termsService.listVersions();
    }

    @PostMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public TermsDtos.AdminTermsVersionResponse publish(
            @AuthenticationPrincipal AuthenticatedUser admin,
            @Valid @RequestBody TermsDtos.PublishTermsVersionRequest request
    ) {
        return termsService.publish(requireUser(admin).userId(), request);
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public TermsDtos.AdminTermsVersionResponse activate(@PathVariable UUID id) {
        return termsService.activate(id);
    }

    @GetMapping("/{id}/acceptances")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public TermsDtos.TermsAcceptancePageResponse acceptances(
            @PathVariable UUID id,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false) OffsetDateTime from,
            @RequestParam(required = false) OffsetDateTime to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "100") @Min(1) @Max(500) int size
    ) {
        return termsService.acceptances(id, userId, deviceId, from, to, page, size);
    }

    private AuthenticatedUser requireUser(AuthenticatedUser user) {
        if (user == null) {
            throw new IllegalArgumentException("Authenticated user is required");
        }
        return user;
    }

    private String resolveIpAddress(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr == null || remoteAddr.isBlank() ? "unknown" : remoteAddr;
    }
}
