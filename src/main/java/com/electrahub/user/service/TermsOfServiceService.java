package com.electrahub.user.service;

import com.electrahub.user.api.dto.AcceptTermsRequest;
import com.electrahub.user.api.dto.AcceptTermsResponse;
import com.electrahub.user.api.dto.AcceptanceStatusResponse;
import com.electrahub.user.api.dto.LinkDeviceRequest;
import com.electrahub.user.api.dto.LinkDeviceResponse;
import com.electrahub.user.api.dto.TermsAcceptanceEntry;
import com.electrahub.user.api.dto.TermsHistoryResponse;
import com.electrahub.user.api.dto.TermsVersionResponse;
import com.electrahub.user.api.error.NotFoundException;
import com.electrahub.user.domain.TermsAcceptance;
import com.electrahub.user.domain.TermsDeviceLink;
import com.electrahub.user.domain.TermsVersion;
import com.electrahub.user.repository.TermsAcceptanceRepository;
import com.electrahub.user.repository.TermsDeviceLinkRepository;
import com.electrahub.user.repository.TermsVersionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TermsOfServiceService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TermsOfServiceService.class);

    private final TermsVersionRepository termsVersionRepository;
    private final TermsAcceptanceRepository termsAcceptanceRepository;
    private final TermsDeviceLinkRepository termsDeviceLinkRepository;

    public TermsOfServiceService(TermsVersionRepository termsVersionRepository,
                                 TermsAcceptanceRepository termsAcceptanceRepository,
                                 TermsDeviceLinkRepository termsDeviceLinkRepository) {
        this.termsVersionRepository = termsVersionRepository;
        this.termsAcceptanceRepository = termsAcceptanceRepository;
        this.termsDeviceLinkRepository = termsDeviceLinkRepository;
    }

    @Transactional(readOnly = true)
    public TermsVersionResponse getCurrentTerms() {
        TermsVersion active = termsVersionRepository.findByIsActiveTrue()
                .orElseThrow(() -> new NotFoundException("No active terms version found"));
        return toVersionResponse(active);
    }

    @Transactional
    public AcceptTermsResponse acceptTerms(AcceptTermsRequest request, UUID userId, String ipAddress) {
        TermsVersion version = termsVersionRepository.findById(request.termsVersionId())
                .orElseThrow(() -> new NotFoundException("Terms version not found: " + request.termsVersionId()));

        if (!version.isActive()) {
            throw new IllegalArgumentException("Terms version is not active: " + request.termsVersionId());
        }

        // Idempotent: if user already accepted this version, return existing
        if (userId != null) {
            Optional<TermsAcceptance> existing = termsAcceptanceRepository
                    .findByUserIdAndTermsVersionId(userId, version.getId());
            if (existing.isPresent()) {
                TermsAcceptance ea = existing.get();
                return new AcceptTermsResponse(ea.getId(), true, ea.getAcceptedAt());
            }
        }

        // Idempotent: if device already accepted this version (anonymous)
        if (request.deviceId() != null && !request.deviceId().isBlank()) {
            Optional<TermsAcceptance> existing = termsAcceptanceRepository
                    .findByDeviceIdAndTermsVersionId(request.deviceId(), version.getId());
            if (existing.isPresent()) {
                TermsAcceptance ea = existing.get();
                // If user is now authenticated, link the existing acceptance
                if (userId != null && ea.getUserId() == null) {
                    ea.setUserId(userId);
                    ea.setLinkedAt(OffsetDateTime.now());
                }
                return new AcceptTermsResponse(ea.getId(), true, ea.getAcceptedAt());
            }
        }

        OffsetDateTime now = OffsetDateTime.now();
        TermsAcceptance acceptance = new TermsAcceptance(UUID.randomUUID(), version, now);
        acceptance.setUserId(userId);
        acceptance.setDeviceId(request.deviceId());
        acceptance.setDevicePlatform(request.devicePlatform());
        acceptance.setDeviceModel(request.deviceModel());
        acceptance.setAppVersion(request.appVersion());
        acceptance.setIpAddress(ipAddress);

        termsAcceptanceRepository.save(acceptance);
        LOGGER.info("Terms accepted: versionId={}, userId={}, deviceId={}", version.getId(), userId, request.deviceId());

        return new AcceptTermsResponse(acceptance.getId(), true, acceptance.getAcceptedAt());
    }

    @Transactional(readOnly = true)
    public AcceptanceStatusResponse getAcceptanceStatus(String deviceId, UUID userId) {
        TermsVersion active = termsVersionRepository.findByIsActiveTrue().orElse(null);
        if (active == null) {
            return new AcceptanceStatusResponse(null, false, null, null, null, false);
        }

        // Check by userId first
        if (userId != null) {
            Optional<TermsAcceptance> acceptance = termsAcceptanceRepository
                    .findByUserIdAndTermsVersionId(userId, active.getId());
            if (acceptance.isPresent()) {
                TermsAcceptance a = acceptance.get();
                return new AcceptanceStatusResponse(
                        active.getId(), true, a.getAcceptedAt(),
                        active.getVersion(), active.getVersion(), false
                );
            }
        }

        // Check by deviceId
        if (deviceId != null && !deviceId.isBlank()) {
            Optional<TermsAcceptance> acceptance = termsAcceptanceRepository
                    .findByDeviceIdAndTermsVersionId(deviceId, active.getId());
            if (acceptance.isPresent()) {
                TermsAcceptance a = acceptance.get();
                return new AcceptanceStatusResponse(
                        active.getId(), true, a.getAcceptedAt(),
                        active.getVersion(), active.getVersion(), false
                );
            }
        }

        // Not accepted
        return new AcceptanceStatusResponse(
                active.getId(), false, null,
                active.getVersion(), null, true
        );
    }

    @Transactional
    public LinkDeviceResponse linkDevice(LinkDeviceRequest request, UUID userId) {
        // Upsert device link
        Optional<TermsDeviceLink> existingLink = termsDeviceLinkRepository
                .findByDeviceIdAndUserId(request.deviceId(), userId);
        if (existingLink.isEmpty()) {
            TermsDeviceLink link = new TermsDeviceLink(
                    UUID.randomUUID(), request.deviceId(), request.devicePlatform(),
                    userId, OffsetDateTime.now()
            );
            termsDeviceLinkRepository.save(link);
        }

        // Find anonymous acceptances for this device and promote them
        List<TermsAcceptance> anonymousAcceptances = termsAcceptanceRepository
                .findByDeviceIdAndUserIdIsNull(request.deviceId());
        OffsetDateTime now = OffsetDateTime.now();
        int linked = 0;
        for (TermsAcceptance acceptance : anonymousAcceptances) {
            // Check if user already has an acceptance for this version
            Optional<TermsAcceptance> existing = termsAcceptanceRepository
                    .findByUserIdAndTermsVersionId(userId, acceptance.getTermsVersion().getId());
            if (existing.isEmpty()) {
                acceptance.setUserId(userId);
                acceptance.setLinkedAt(now);
                linked++;
            }
        }

        LOGGER.info("Linked {} device acceptances: deviceId={}, userId={}", linked, request.deviceId(), userId);
        return new LinkDeviceResponse(linked);
    }

    @Transactional(readOnly = true)
    public TermsHistoryResponse getHistory(UUID userId) {
        List<TermsAcceptance> acceptances = termsAcceptanceRepository
                .findByUserIdOrderByAcceptedAtDesc(userId);
        List<TermsAcceptanceEntry> entries = acceptances.stream()
                .map(a -> new TermsAcceptanceEntry(
                        a.getTermsVersion().getId(),
                        a.getTermsVersion().getVersion(),
                        a.getAcceptedAt(),
                        a.getDevicePlatform(),
                        a.getLinkedAt() != null
                ))
                .toList();
        return new TermsHistoryResponse(entries);
    }

    private TermsVersionResponse toVersionResponse(TermsVersion v) {
        return new TermsVersionResponse(
                v.getId(), v.getVersion(), v.getTitle(), v.getContentUrl(),
                v.getSummary(), v.getEffectiveDate(), v.getEnforcementMode(),
                v.getMetadata()
        );
    }
}
