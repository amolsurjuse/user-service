package com.electrahub.user.repository;

import com.electrahub.user.domain.TermsAcceptance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TermsAcceptanceRepository extends JpaRepository<TermsAcceptance, UUID> {

    Optional<TermsAcceptance> findByUserIdAndTermsVersionId(UUID userId, UUID termsVersionId);

    Optional<TermsAcceptance> findByDeviceIdAndTermsVersionId(String deviceId, UUID termsVersionId);

    List<TermsAcceptance> findByDeviceIdAndUserIdIsNull(String deviceId);

    List<TermsAcceptance> findByUserIdOrderByAcceptedAtDesc(UUID userId);
}
