package com.electrahub.user.service;

import com.electrahub.user.api.dto.ChargerPreferenceDtos;
import com.electrahub.user.domain.UserChargerPreference;
import com.electrahub.user.repository.UserChargerPreferenceRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class ChargerPreferenceService {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };
    private static final TypeReference<List<ChargerPreferenceDtos.ConnectorSummary>> CONNECTOR_LIST =
            new TypeReference<>() {
            };

    private final UserChargerPreferenceRepository repository;
    private final ObjectMapper objectMapper;

    public ChargerPreferenceService(UserChargerPreferenceRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public ChargerPreferenceDtos.PreferencesResponse preferences(UUID userId) {
        return new ChargerPreferenceDtos.PreferencesResponse(
                repository.findTop10ByUserIdAndLastUsedAtIsNotNullOrderByLastUsedAtDesc(userId).stream()
                        .map(this::toResponse)
                        .toList(),
                repository.findByUserIdAndFavoriteTrueOrderByUpdatedAtDesc(userId).stream()
                        .map(this::toResponse)
                        .toList()
        );
    }

    @Transactional
    public ChargerPreferenceDtos.PreferencesResponse recordRecent(
            UUID userId,
            ChargerPreferenceDtos.StationPreferenceRequest request
    ) {
        UserChargerPreference preference = upsertSnapshot(userId, request);
        preference.markRecentlyUsed();
        repository.save(preference);
        return preferences(userId);
    }

    @Transactional
    public ChargerPreferenceDtos.PreferencesResponse addFavorite(
            UUID userId,
            ChargerPreferenceDtos.StationPreferenceRequest request
    ) {
        UserChargerPreference preference = upsertSnapshot(userId, request);
        preference.setFavorite(true);
        repository.save(preference);
        return preferences(userId);
    }

    @Transactional
    public ChargerPreferenceDtos.PreferencesResponse removeFavorite(UUID userId, String stationId) {
        repository.findByUserIdAndStationId(userId, stationId)
                .ifPresent(preference -> {
                    preference.setFavorite(false);
                    repository.save(preference);
                });
        return preferences(userId);
    }

    private UserChargerPreference upsertSnapshot(
            UUID userId,
            ChargerPreferenceDtos.StationPreferenceRequest request
    ) {
        UserChargerPreference preference = repository.findByUserIdAndStationId(userId, request.id())
                .orElseGet(() -> new UserChargerPreference(UUID.randomUUID(), userId, request.id()));
        preference.updateStationSnapshot(
                request.name(),
                blankToEmpty(request.address()),
                blankToEmpty(request.city()),
                blankToEmpty(request.state()),
                blankToEmpty(request.postalCode()),
                BigDecimal.valueOf(request.latitude()),
                BigDecimal.valueOf(request.longitude()),
                defaultString(request.status(), "AVAILABLE"),
                request.operatingHours(),
                request.imageURL(),
                writeJson(defaultList(request.chargerIds())),
                writeJson(defaultList(request.connectors()))
        );
        return preference;
    }

    private ChargerPreferenceDtos.StationPreferenceResponse toResponse(UserChargerPreference preference) {
        return new ChargerPreferenceDtos.StationPreferenceResponse(
                preference.getStationId(),
                preference.getStationName(),
                preference.getAddress(),
                preference.getCity(),
                preference.getState(),
                preference.getPostalCode(),
                preference.getLatitude() == null ? 0.0 : preference.getLatitude().doubleValue(),
                preference.getLongitude() == null ? 0.0 : preference.getLongitude().doubleValue(),
                readJson(preference.getConnectorsJson(), CONNECTOR_LIST),
                preference.getStatus(),
                preference.getOperatingHours(),
                preference.getImageUrl(),
                readJson(preference.getChargerIdsJson(), STRING_LIST),
                preference.isFavorite(),
                preference.getLastUsedAt()
        );
    }

    private <T> String writeJson(List<T> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unable to serialize charger preference payload", e);
        }
    }

    private <T> List<T> readJson(String json, TypeReference<List<T>> type) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private static <T> List<T> defaultList(List<T> value) {
        return value == null ? List.of() : value;
    }

    private static String blankToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
