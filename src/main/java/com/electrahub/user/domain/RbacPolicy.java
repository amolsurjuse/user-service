package com.electrahub.user.domain;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "rbac_policies")
public class RbacPolicy {
    private static final Logger LOGGER = LoggerFactory.getLogger(RbacPolicy.class);


    @Id
    private UUID id;

    @Column(name = "policy_key", nullable = false, unique = true, length = 64)
    private String policyKey;

    @Column(name = "role_hierarchy", nullable = false)
    private String roleHierarchy;

    @Column(name = "default_decision", nullable = false, length = 8)
    private String defaultDecision;

    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    private List<RbacPolicyRule> rules = new ArrayList<>();

    /**
     * Executes rbac policy for `RbacPolicy`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     */
    protected RbacPolicy() {
        LOGGER.info("CODEx_ENTRY_LOG: Entering RbacPolicy#RbacPolicy");
        LOGGER.debug("CODEx_ENTRY_LOG: Entering RbacPolicy#RbacPolicy with debug context");
    }

    /**
     * Executes rbac policy for `RbacPolicy`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @param id input consumed by RbacPolicy.
     * @param policyKey input consumed by RbacPolicy.
     * @param roleHierarchy input consumed by RbacPolicy.
     * @param defaultDecision input consumed by RbacPolicy.
     * @param version input consumed by RbacPolicy.
     */
    public RbacPolicy(UUID id, String policyKey, String roleHierarchy, String defaultDecision, long version) {
        this.id = id;
        this.policyKey = policyKey;
        this.roleHierarchy = roleHierarchy;
        this.defaultDecision = defaultDecision;
        this.version = version;
        this.updatedAt = OffsetDateTime.now();
    }

    /**
     * Executes touch timestamp for `RbacPolicy`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     */
    @PrePersist
    @PreUpdate
    void touchTimestamp() {
        this.updatedAt = OffsetDateTime.now();
    }

    /**
     * Retrieves get id for `RbacPolicy`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @return result produced by getId.
     */
    public UUID getId() {
        return id;
    }

    /**
     * Retrieves get policy key for `RbacPolicy`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @return result produced by getPolicyKey.
     */
    public String getPolicyKey() {
        return policyKey;
    }

    /**
     * Retrieves get role hierarchy for `RbacPolicy`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @return result produced by getRoleHierarchy.
     */
    public String getRoleHierarchy() {
        return roleHierarchy;
    }

    /**
     * Retrieves get default decision for `RbacPolicy`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @return result produced by getDefaultDecision.
     */
    public String getDefaultDecision() {
        return defaultDecision;
    }

    /**
     * Retrieves get version for `RbacPolicy`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @return result produced by getVersion.
     */
    public long getVersion() {
        return version;
    }

    /**
     * Retrieves get updated at for `RbacPolicy`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @return result produced by getUpdatedAt.
     */
    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Retrieves get rules for `RbacPolicy`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @return result produced by getRules.
     */
    public List<RbacPolicyRule> getRules() {
        return rules;
    }

    /**
     * Updates set role hierarchy for `RbacPolicy`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @param roleHierarchy input consumed by setRoleHierarchy.
     */
    public void setRoleHierarchy(String roleHierarchy) {
        this.roleHierarchy = roleHierarchy;
    }

    /**
     * Updates set default decision for `RbacPolicy`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @param defaultDecision input consumed by setDefaultDecision.
     */
    public void setDefaultDecision(String defaultDecision) {
        this.defaultDecision = defaultDecision;
    }

    /**
     * Executes replace rules for `RbacPolicy`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @param nextRules input consumed by replaceRules.
     */
    public void replaceRules(List<RbacPolicyRule> nextRules) {
        rules.clear();
        if (nextRules == null || nextRules.isEmpty()) {
            return;
        }
        nextRules.forEach(this::addRule);
    }

    /**
     * Creates add rule for `RbacPolicy`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @param rule input consumed by addRule.
     */
    public void addRule(RbacPolicyRule rule) {
        rule.setPolicy(this);
        rules.add(rule);
    }

    /**
     * Executes bump version for `RbacPolicy`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     */
    public void bumpVersion() {
        this.version = this.version + 1;
    }
}
