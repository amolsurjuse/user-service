package com.electrahub.user.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;

public final class ChargerPreferenceDtos {

    private ChargerPreferenceDtos() {
    }

    public record PreferencesResponse(
            List<StationPreferenceResponse> recentStations,
            List<StationPreferenceResponse> favoriteStations
    ) {
    }

    public record StationPreferenceRequest(
            @NotBlank @Size(max = 128) String id,
            @NotBlank @Size(max = 255) String name,
            @Size(max = 512) String address,
            @Size(max = 128) String city,
            @Size(max = 64) String state,
            @Size(max = 32) String postalCode,
            @DecimalMin("-90.0") @DecimalMax("90.0") double latitude,
            @DecimalMin("-180.0") @DecimalMax("180.0") double longitude,
            @Valid @Size(max = 20) List<ConnectorSummary> connectors,
            @Size(max = 32) String status,
            @Size(max = 128) String operatingHours,
            @Size(max = 1024) String imageURL,
            @Size(max = 20) List<@Size(max = 128) String> chargerIds
    ) {
    }

    public record StationPreferenceResponse(
            String id,
            String name,
            String address,
            String city,
            String state,
            String postalCode,
            double latitude,
            double longitude,
            List<ConnectorSummary> connectors,
            String status,
            String operatingHours,
            String imageURL,
            List<String> chargerIds,
            boolean favorite,
            OffsetDateTime lastUsedAt
    ) {
    }

    public record ConnectorSummary(
            @NotBlank @Size(max = 128) String id,
            @Size(max = 32) String type,
            int power,
            @Size(max = 32) String status,
            double tariffPerKwh
    ) {
    }
}
