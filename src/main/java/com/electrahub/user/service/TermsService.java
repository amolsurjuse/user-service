package com.electrahub.user.service;

import com.electrahub.user.api.dto.TermsDtos;
import com.electrahub.user.api.error.NotFoundException;
import com.electrahub.user.domain.TermsAcceptance;
import com.electrahub.user.domain.TermsVersion;
import com.electrahub.user.repository.TermsAcceptanceRepository;
import com.electrahub.user.repository.TermsVersionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class TermsService {
    private static final String DRIVER_CONTENT_TEXT = """
            ElectraHub Driver App Terms and Conditions

            Effective May 2026

            By using the ElectraHub Driver App, you agree to use the app for personal electric vehicle charging, account management, payment, wallet, charging history, and support activities.

            You are responsible for protecting your account, securing your device, and reporting suspicious activity. You may not misuse charging equipment, attempt to bypass authorization or payment controls, interfere with other drivers' sessions, or use the app in a way that disrupts ElectraHub services.

            ElectraHub stores driver profile data, account status, authentication audit details, charging session records, station and connector activity, pricing and billing records, payment transaction references, wallet activity, terms acceptance history, device details, IP address, and user agent details needed for service delivery, security, billing, and compliance.

            Driver data is stored in ElectraHub managed backend systems, including application databases, audit logs, operational indexes, and secure infrastructure used to run the ElectraHub platform. Payment data is processed according to the selected payment method and may be stored as transaction references, settlement records, and audit history.

            ElectraHub may use this data to provide charging services, administer accounts, secure the platform, investigate incidents, meet legal obligations, calculate pricing and settlement, support customer requests, and maintain audit trails.

            Continued use of the Driver App confirms that you have read and accepted these terms.
            """;

    private final TermsVersionRepository termsVersionRepository;
    private final TermsAcceptanceRepository termsAcceptanceRepository;

    public TermsService(
            TermsVersionRepository termsVersionRepository,
            TermsAcceptanceRepository termsAcceptanceRepository
    ) {
        this.termsVersionRepository = termsVersionRepository;
        this.termsAcceptanceRepository = termsAcceptanceRepository;
    }

    @Transactional(readOnly = true)
    public TermsDtos.TermsVersionResponse currentTerms() {
        return currentTerms(TermsAudience.DRIVER_PORTAL);
    }

    @Transactional(readOnly = true)
    public TermsDtos.TermsVersionResponse currentTerms(TermsAudience audience) {
        return toVersionResponse(activeVersion(), audience);
    }

    @Transactional(readOnly = true)
    public TermsDtos.TermsStatusResponse status(UUID userId) {
        TermsVersion active = activeVersion();
        Integer acceptedVersion = latestAcceptedVersionNumber(userId);
        boolean accepted = isAccepted(userId, active);
        return new TermsDtos.TermsStatusResponse(accepted, active.getVersionNumber(), acceptedVersion);
    }

    @Transactional(readOnly = true)
    public TermsDtos.TermsGateStatusResponse gateStatus(UUID userId) {
        return gateStatus(userId, TermsAudience.DRIVER_PORTAL);
    }

    @Transactional(readOnly = true)
    public TermsDtos.TermsGateStatusResponse gateStatus(UUID userId, TermsAudience audience) {
        TermsVersion active = activeVersion();
        Integer acceptedVersion = latestAcceptedVersionNumber(userId);
        boolean accepted = isAccepted(userId, active);
        return new TermsDtos.TermsGateStatusResponse(
                accepted,
                active.getVersionNumber(),
                acceptedVersion,
                active.getVersionLabel(),
                active.getContentUrl(),
                active.getContentSha256(),
                contentTextForAudience(active, audience)
        );
    }

    @Transactional
    public TermsDtos.TermsAcceptResponse acceptCurrent(
            UUID userId,
            TermsDtos.TermsAcceptRequest request,
            String ipAddress,
            String userAgent
    ) {
        TermsVersion active = activeVersion();
        return termsAcceptanceRepository.findByUserIdAndTermsVersionId(userId, active.getId())
                .map(existing -> new TermsDtos.TermsAcceptResponse(
                        existing.getId(),
                        existing.getAcceptedAt(),
                        existing.getTermsVersion().getVersionNumber()
                ))
                .orElseGet(() -> createAcceptance(userId, active, request, ipAddress, userAgent));
    }

    @Transactional(readOnly = true)
    public List<TermsDtos.TermsAcceptanceResponse> history(UUID userId) {
        return termsAcceptanceRepository.findByUserIdOrderByAcceptedAtDesc(userId)
                .stream()
                .map(this::toAcceptanceResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TermsDtos.AdminTermsVersionResponse> listVersions() {
        return termsVersionRepository.findAll().stream()
                .sorted((left, right) -> Integer.compare(right.getVersionNumber(), left.getVersionNumber()))
                .map(this::toAdminVersionResponse)
                .toList();
    }

    @Transactional
    public TermsDtos.AdminTermsVersionResponse publish(UUID adminUserId, TermsDtos.PublishTermsVersionRequest request) {
        int nextVersionNumber = termsVersionRepository.findTopByOrderByVersionNumberDesc()
                .map(TermsVersion::getVersionNumber)
                .orElse(0) + 1;
        OffsetDateTime now = OffsetDateTime.now();
        TermsVersion version = new TermsVersion(
                UUID.randomUUID(),
                nextVersionNumber,
                request.versionLabel().trim(),
                request.contentUrl().trim(),
                request.contentSha256().trim().toLowerCase(Locale.ROOT),
                request.contentText().trim(),
                request.requiresReAcceptance(),
                request.effectiveDate() == null ? now : request.effectiveDate(),
                adminUserId,
                now
        );
        TermsVersion saved = termsVersionRepository.save(version);
        return toAdminVersionResponse(saved);
    }

    @Transactional
    public TermsDtos.AdminTermsVersionResponse activate(UUID termsVersionId) {
        TermsVersion target = termsVersionRepository.findById(termsVersionId)
                .orElseThrow(() -> new NotFoundException("Terms version not found: " + termsVersionId));
        termsVersionRepository.findActiveForUpdate().ifPresent(TermsVersion::deactivate);
        target.activate();
        return toAdminVersionResponse(termsVersionRepository.save(target));
    }

    @Transactional(readOnly = true)
    public TermsDtos.TermsAcceptancePageResponse acceptances(
            UUID termsVersionId,
            UUID userId,
            String deviceId,
            OffsetDateTime from,
            OffsetDateTime to,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        var result = termsAcceptanceRepository.searchAcceptances(termsVersionId, userId, deviceId, from, to, pageable);
        List<TermsDtos.TermsAcceptanceResponse> items = result.getContent().stream()
                .map(this::toAcceptanceResponse)
                .toList();
        return new TermsDtos.TermsAcceptancePageResponse(
                items,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Transactional
    public void activateDueVersions() {
        OffsetDateTime now = OffsetDateTime.now();
        List<TermsVersion> dueVersions = termsVersionRepository
                .findByEffectiveDateLessThanEqualAndActiveFalseOrderByEffectiveDateAsc(now);
        for (TermsVersion dueVersion : dueVersions) {
            termsVersionRepository.findActiveForUpdate().ifPresent(TermsVersion::deactivate);
            dueVersion.activate();
        }
    }

    private TermsDtos.TermsAcceptResponse createAcceptance(
            UUID userId,
            TermsVersion active,
            TermsDtos.TermsAcceptRequest request,
            String ipAddress,
            String userAgent
    ) {
        TermsAcceptance acceptance = new TermsAcceptance(
                UUID.randomUUID(),
                userId,
                active,
                OffsetDateTime.now(),
                request.deviceId().trim(),
                request.deviceModel().trim(),
                request.osVersion().trim(),
                request.appVersion().trim(),
                request.platform().trim(),
                ipAddress,
                userAgent == null ? "" : userAgent
        );
        TermsAcceptance saved;
        try {
            saved = termsAcceptanceRepository.saveAndFlush(acceptance);
        } catch (DataIntegrityViolationException ex) {
            saved = termsAcceptanceRepository.findByUserIdAndTermsVersionId(userId, active.getId())
                    .orElseThrow(() -> ex);
        }
        return new TermsDtos.TermsAcceptResponse(
                saved.getId(),
                saved.getAcceptedAt(),
                saved.getTermsVersion().getVersionNumber()
        );
    }

    private TermsVersion activeVersion() {
        return termsVersionRepository.findByActiveTrue()
                .orElseThrow(() -> new NotFoundException("No active Terms version is configured"));
    }

    private boolean isAccepted(UUID userId, TermsVersion active) {
        if (!active.isRequiresReAcceptance()) {
            return termsAcceptanceRepository.findTopByUserIdOrderByTermsVersionVersionNumberDescAcceptedAtDesc(userId)
                    .isPresent();
        }
        return termsAcceptanceRepository.findByUserIdAndTermsVersionId(userId, active.getId()).isPresent();
    }

    private Integer latestAcceptedVersionNumber(UUID userId) {
        return termsAcceptanceRepository.findTopByUserIdOrderByTermsVersionVersionNumberDescAcceptedAtDesc(userId)
                .map(acceptance -> acceptance.getTermsVersion().getVersionNumber())
                .orElse(null);
    }

    private TermsDtos.TermsVersionResponse toVersionResponse(TermsVersion version, TermsAudience audience) {
        return new TermsDtos.TermsVersionResponse(
                version.getId(),
                version.getVersionNumber(),
                version.getVersionLabel(),
                version.getContentUrl(),
                version.getContentSha256(),
                contentTextForAudience(version, audience),
                version.isRequiresReAcceptance(),
                version.getEffectiveDate()
        );
    }

    private TermsDtos.AdminTermsVersionResponse toAdminVersionResponse(TermsVersion version) {
        return new TermsDtos.AdminTermsVersionResponse(
                version.getId(),
                version.getVersionNumber(),
                version.getVersionLabel(),
                version.getContentUrl(),
                version.getContentSha256(),
                normalizeContentText(version.getContentText()),
                version.isRequiresReAcceptance(),
                version.getEffectiveDate(),
                version.getPublishedBy(),
                version.getCreatedAt(),
                version.isActive(),
                termsAcceptanceRepository.countByTermsVersionId(version.getId())
        );
    }

    private String normalizeContentText(String contentText) {
        return contentText == null ? "" : contentText;
    }

    private String contentTextForAudience(TermsVersion version, TermsAudience audience) {
        String contentText = normalizeContentText(version.getContentText()).trim();
        if (audience == TermsAudience.DRIVER_PORTAL && (contentText.isBlank() || looksAdminSpecific(contentText))) {
            return DRIVER_CONTENT_TEXT.trim();
        }
        return contentText;
    }

    private boolean looksAdminSpecific(String contentText) {
        String normalized = contentText.toLowerCase(Locale.ROOT);
        return normalized.contains("admin portal") || normalized.contains("administrative tools");
    }

    private TermsDtos.TermsAcceptanceResponse toAcceptanceResponse(TermsAcceptance acceptance) {
        TermsVersion version = acceptance.getTermsVersion();
        return new TermsDtos.TermsAcceptanceResponse(
                acceptance.getId(),
                acceptance.getUserId(),
                version.getVersionNumber(),
                version.getVersionLabel(),
                acceptance.getAcceptedAt(),
                acceptance.getDeviceId(),
                acceptance.getDeviceModel(),
                acceptance.getOsVersion(),
                acceptance.getAppVersion(),
                acceptance.getPlatform(),
                acceptance.getIpAddress(),
                acceptance.getUserAgent()
        );
    }
}
