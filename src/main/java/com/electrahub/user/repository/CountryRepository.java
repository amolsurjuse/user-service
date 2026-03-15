package com.electrahub.user.repository;

import com.electrahub.user.domain.Country;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CountryRepository extends JpaRepository<Country, UUID> {

    Optional<Country> findByIsoCodeAndEnabledTrue(String isoCode);

    List<Country> findByEnabledTrueOrderByNameAsc();
}
