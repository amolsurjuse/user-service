package com.electrahub.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "rbac_policy_rules")
public class RbacPolicyRule {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false)
    private RbacPolicy policy;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 200)
    private String methods;

    @Column(name = "path_pattern", nullable = false, length = 255)
    private String pathPattern;

    @Column(nullable = false, length = 8)
    private String effect;

    @Column(name = "allow_anonymous", nullable = false)
    private boolean allowAnonymous;

    @Column(name = "required_roles", length = 400)
    private String requiredRoles;

    protected RbacPolicyRule() {
    }

    public RbacPolicyRule(
            UUID id,
            int sortOrder,
            String name,
            String methods,
            String pathPattern,
            String effect,
            boolean allowAnonymous,
            String requiredRoles
    ) {
        this.id = id;
        this.sortOrder = sortOrder;
        this.name = name;
        this.methods = methods;
        this.pathPattern = pathPattern;
        this.effect = effect;
        this.allowAnonymous = allowAnonymous;
        this.requiredRoles = requiredRoles;
    }

    public UUID getId() {
        return id;
    }

    public RbacPolicy getPolicy() {
        return policy;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public String getName() {
        return name;
    }

    public String getMethods() {
        return methods;
    }

    public String getPathPattern() {
        return pathPattern;
    }

    public String getEffect() {
        return effect;
    }

    public boolean isAllowAnonymous() {
        return allowAnonymous;
    }

    public String getRequiredRoles() {
        return requiredRoles;
    }

    public void setPolicy(RbacPolicy policy) {
        this.policy = policy;
    }
}
