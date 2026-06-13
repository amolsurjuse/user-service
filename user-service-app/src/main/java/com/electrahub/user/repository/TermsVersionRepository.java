package com.electrahub.user.repository;

import com.electrahub.user.domain.TermsVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TermsVersionRepository extends JpaRepository<TermsVersion, UUID> {

    Optional<TermsVersion> findByActiveTrue();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select tv from TermsVersion tv where tv.active = true")
    Optional<TermsVersion> findActiveForUpdate();

    Optional<TermsVersion> findTopByOrderByVersionNumberDesc();

    List<TermsVersion> findByEffectiveDateLessThanEqualAndActiveFalseOrderByEffectiveDateAsc(OffsetDateTime now);
}
