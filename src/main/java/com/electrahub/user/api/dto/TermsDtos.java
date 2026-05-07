package com.electrahub.user.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class TermsDtos {

    private TermsDtos() {
    }

    public record TermsVersionResponse(
            UUID id,
            int versionNumber,
            String versionLabel,
            String contentUrl,
            String contentSha256,
            boolean requiresReAcceptance,
            OffsetDateTime effectiveDate
    ) {
    }

    public record TermsStatusResponse(
            boolean termsAccepted,
            int currentVersionNumber,
            Integer acceptedVersionNumber
    ) {
    }

    public record TermsAcceptRequest(
            @NotBlank @Size(max = 128) String deviceId,
            @NotBlank @Size(max = 64) String deviceModel,
            @NotBlank @Size(max = 32) String osVersion,
            @NotBlank @Size(max = 32) String appVersion,
            @NotBlank @Pattern(regexp = "iOS|Android|Web") String platform
    ) {
    }

    public record TermsAcceptResponse(
            UUID acceptanceId,
            OffsetDateTime acceptedAt,
            int termsVersionNumber
    ) {
    }

    public record TermsAcceptanceResponse(
            UUID acceptanceId,
            UUID userId,
            int termsVersionNumber,
            String termsVersionLabel,
            OffsetDateTime acceptedAt,
            String deviceId,
            String deviceModel,
            String osVersion,
            String appVersion,
            String platform,
            String ipAddress,
            String userAgent
    ) {
    }

    public record AdminTermsVersionResponse(
            UUID id,
            int versionNumber,
            String versionLabel,
            String contentUrl,
            String contentSha256,
            boolean requiresReAcceptance,
            OffsetDateTime effectiveDate,
            UUID publishedBy,
            OffsetDateTime createdAt,
            boolean active,
            long acceptanceCount
    ) {
    }

    public record PublishTermsVersionRequest(
            @NotBlank @Size(max = 32) String versionLabel,
            @NotBlank String contentUrl,
            @NotBlank @Pattern(regexp = "^[0-9a-fA-F]{64}$") String contentSha256,
            boolean requiresReAcceptance,
            OffsetDateTime effectiveDate
    ) {
    }

    public record TermsGateStatusResponse(
            boolean termsAccepted,
            int currentVersionNumber,
            Integer acceptedVersionNumber,
            String currentVersionLabel,
            String contentUrl,
            String contentSha256
    ) {
    }

    public record TermsAcceptancePageResponse(
            List<TermsAcceptanceResponse> items,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
    }
}
