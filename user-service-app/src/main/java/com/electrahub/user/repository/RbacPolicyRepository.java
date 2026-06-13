package com.electrahub.user.repository;

import com.electrahub.user.domain.RbacPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RbacPolicyRepository extends JpaRepository<RbacPolicy, UUID> {
    /**
     * Retrieves find by policy key for `RbacPolicyRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.repository`.
     * @param policyKey input consumed by findByPolicyKey.
     * @return result produced by findByPolicyKey.
     */
    Optional<RbacPolicy> findByPolicyKey(String policyKey);
}
