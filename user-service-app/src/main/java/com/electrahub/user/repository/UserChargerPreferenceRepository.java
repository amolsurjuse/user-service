package com.electrahub.user.repository;

import com.electrahub.user.domain.UserChargerPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserChargerPreferenceRepository extends JpaRepository<UserChargerPreference, UUID> {

    Optional<UserChargerPreference> findByUserIdAndStationId(UUID userId, String stationId);

    List<UserChargerPreference> findTop10ByUserIdAndLastUsedAtIsNotNullOrderByLastUsedAtDesc(UUID userId);

    List<UserChargerPreference> findByUserIdAndFavoriteTrueOrderByUpdatedAtDesc(UUID userId);
}
