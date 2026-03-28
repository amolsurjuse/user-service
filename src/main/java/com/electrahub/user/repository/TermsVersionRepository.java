package com.electrahub.user.repository;

import com.electrahub.user.domain.TermsVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TermsVersionRepository extends JpaRepository<TermsVersion, UUID> {

    Optional<TermsVersion> findByIsActiveTrue();

    List<TermsVersion> findAllByOrderByCreatedAtDesc();
}
