package com.electrahub.user.domain;

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

    protected RbacPolicy() {
    }

    public RbacPolicy(UUID id, String policyKey, String roleHierarchy, String defaultDecision, long version) {
        this.id = id;
        this.policyKey = policyKey;
        this.roleHierarchy = roleHierarchy;
        this.defaultDecision = defaultDecision;
        this.version = version;
        this.updatedAt = OffsetDateTime.now();
    }

    @PrePersist
    @PreUpdate
    void touchTimestamp() {
        this.updatedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public String getPolicyKey() {
        return policyKey;
    }

    public String getRoleHierarchy() {
        return roleHierarchy;
    }

    public String getDefaultDecision() {
        return defaultDecision;
    }

    public long getVersion() {
        return version;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<RbacPolicyRule> getRules() {
        return rules;
    }

    public void setRoleHierarchy(String roleHierarchy) {
        this.roleHierarchy = roleHierarchy;
    }

    public void setDefaultDecision(String defaultDecision) {
        this.defaultDecision = defaultDecision;
    }

    public void replaceRules(List<RbacPolicyRule> nextRules) {
        rules.clear();
        if (nextRules == null || nextRules.isEmpty()) {
            return;
        }
        nextRules.forEach(this::addRule);
    }

    public void addRule(RbacPolicyRule rule) {
        rule.setPolicy(this);
        rules.add(rule);
    }

    public void bumpVersion() {
        this.version = this.version + 1;
    }
}
