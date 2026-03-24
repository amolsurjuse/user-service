package com.electrahub.user.domain;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(RbacPolicyRule.class);


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

    /**
     * Executes rbac policy rule for `RbacPolicyRule`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     */
    protected RbacPolicyRule() {
        LOGGER.info("CODEx_ENTRY_LOG: Entering RbacPolicyRule#RbacPolicyRule");
        LOGGER.debug("CODEx_ENTRY_LOG: Entering RbacPolicyRule#RbacPolicyRule with debug context");
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

    /**
     * Retrieves get id for `RbacPolicyRule`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @return result produced by getId.
     */
    public UUID getId() {
        return id;
    }

    /**
     * Retrieves get policy for `RbacPolicyRule`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @return result produced by getPolicy.
     */
    public RbacPolicy getPolicy() {
        return policy;
    }

    /**
     * Retrieves get sort order for `RbacPolicyRule`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @return result produced by getSortOrder.
     */
    public int getSortOrder() {
        return sortOrder;
    }

    /**
     * Retrieves get name for `RbacPolicyRule`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @return result produced by getName.
     */
    public String getName() {
        return name;
    }

    /**
     * Retrieves get methods for `RbacPolicyRule`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @return result produced by getMethods.
     */
    public String getMethods() {
        return methods;
    }

    /**
     * Retrieves get path pattern for `RbacPolicyRule`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @return result produced by getPathPattern.
     */
    public String getPathPattern() {
        return pathPattern;
    }

    /**
     * Retrieves get effect for `RbacPolicyRule`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @return result produced by getEffect.
     */
    public String getEffect() {
        return effect;
    }

    /**
     * Executes is allow anonymous for `RbacPolicyRule`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @return result produced by isAllowAnonymous.
     */
    public boolean isAllowAnonymous() {
        return allowAnonymous;
    }

    /**
     * Retrieves get required roles for `RbacPolicyRule`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @return result produced by getRequiredRoles.
     */
    public String getRequiredRoles() {
        return requiredRoles;
    }

    /**
     * Updates set policy for `RbacPolicyRule`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @param policy input consumed by setPolicy.
     */
    public void setPolicy(RbacPolicy policy) {
        this.policy = policy;
    }
}
