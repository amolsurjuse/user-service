package com.electrahub.user.repository;

import com.electrahub.user.domain.AdminScopeGrant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AdminScopeGrantRepository extends JpaRepository<AdminScopeGrant, UUID> {

    List<AdminScopeGrant> findByUserIdOrderByScopeTypeAscScopeIdAsc(UUID userId);

    void deleteByUserId(UUID userId);
}
