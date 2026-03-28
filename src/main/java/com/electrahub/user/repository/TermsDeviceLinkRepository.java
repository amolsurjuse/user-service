package com.electrahub.user.repository;

import com.electrahub.user.domain.TermsDeviceLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TermsDeviceLinkRepository extends JpaRepository<TermsDeviceLink, UUID> {

    Optional<TermsDeviceLink> findByDeviceIdAndUserId(String deviceId, UUID userId);
}
