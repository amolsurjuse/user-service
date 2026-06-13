package com.electrahub.user.repository;

import com.electrahub.user.domain.TermsAcceptance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TermsAcceptanceRepository extends JpaRepository<TermsAcceptance, UUID> {

    Optional<TermsAcceptance> findByUserIdAndTermsVersionId(UUID userId, UUID termsVersionId);

    List<TermsAcceptance> findByUserIdOrderByAcceptedAtDesc(UUID userId);

    Optional<TermsAcceptance> findTopByUserIdOrderByTermsVersionVersionNumberDescAcceptedAtDesc(UUID userId);

    long countByTermsVersionId(UUID termsVersionId);

    @Query("""
            select ta from TermsAcceptance ta
            where ta.termsVersion.id = :termsVersionId
              and (:userId is null or ta.userId = :userId)
              and (:deviceId is null or ta.deviceId = :deviceId)
              and (:from is null or ta.acceptedAt >= :from)
              and (:to is null or ta.acceptedAt <= :to)
            order by ta.acceptedAt desc
            """)
    Page<TermsAcceptance> searchAcceptances(
            UUID termsVersionId,
            UUID userId,
            String deviceId,
            OffsetDateTime from,
            OffsetDateTime to,
            Pageable pageable
    );
}
