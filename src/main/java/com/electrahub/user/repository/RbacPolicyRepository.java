package com.electrahub.user.repository;

import com.electrahub.user.domain.RbacPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RbacPolicyRepository extends JpaRepository<RbacPolicy, UUID> {
    Optional<RbacPolicy> findByPolicyKey(String policyKey);
}
