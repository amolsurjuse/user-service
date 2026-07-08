package com.electrahub.user.api;

import com.electrahub.user.api.dto.ChargerPreferenceDtos;
import com.electrahub.user.security.AuthenticatedUser;
import com.electrahub.user.service.ChargerPreferenceService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/charger-preferences")
public class ChargerPreferenceController {

    private final ChargerPreferenceService chargerPreferenceService;

    public ChargerPreferenceController(ChargerPreferenceService chargerPreferenceService) {
        this.chargerPreferenceService = chargerPreferenceService;
    }

    @GetMapping
    public ChargerPreferenceDtos.PreferencesResponse preferences(@AuthenticationPrincipal AuthenticatedUser user) {
        return chargerPreferenceService.preferences(requireUser(user).userId());
    }

    @PostMapping("/recent")
    public ChargerPreferenceDtos.PreferencesResponse recordRecent(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody ChargerPreferenceDtos.StationPreferenceRequest request
    ) {
        return chargerPreferenceService.recordRecent(requireUser(user).userId(), request);
    }

    @PostMapping("/favorites")
    public ChargerPreferenceDtos.PreferencesResponse addFavorite(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody ChargerPreferenceDtos.StationPreferenceRequest request
    ) {
        return chargerPreferenceService.addFavorite(requireUser(user).userId(), request);
    }

    @DeleteMapping("/favorites/{stationId}")
    public ChargerPreferenceDtos.PreferencesResponse removeFavorite(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String stationId
    ) {
        return chargerPreferenceService.removeFavorite(requireUser(user).userId(), stationId);
    }

    private AuthenticatedUser requireUser(AuthenticatedUser user) {
        if (user == null) {
            throw new IllegalArgumentException("Authenticated user is required");
        }
        return user;
    }
}
