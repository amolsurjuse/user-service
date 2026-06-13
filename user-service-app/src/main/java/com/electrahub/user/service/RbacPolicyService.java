package com.electrahub.user.service;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.electrahub.user.api.dto.GatewayRbacPolicyResponse;
import com.electrahub.user.api.dto.GatewayRbacRuleResponse;
import com.electrahub.user.api.dto.RbacPolicyResponse;
import com.electrahub.user.api.dto.RbacPolicyUpdateRequest;
import com.electrahub.user.api.dto.RbacRuleRequest;
import com.electrahub.user.api.dto.RbacRuleResponse;
import com.electrahub.user.config.RbacSyncProperties;
import com.electrahub.user.domain.RbacPolicy;
import com.electrahub.user.domain.RbacPolicyRule;
import com.electrahub.user.repository.RbacPolicyRepository;
import com.electrahub.user.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RbacPolicyService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RbacPolicyService.class);


    private static final Set<String> VALID_DECISIONS = Set.of("ALLOW", "DENY");

    private final RbacPolicyRepository rbacPolicyRepository;
    private final RoleRepository roleRepository;
    private final RbacSyncProperties rbacSyncProperties;
    private final GatewayRbacCacheInvalidationClient gatewayRbacCacheInvalidationClient;

    public RbacPolicyService(
            RbacPolicyRepository rbacPolicyRepository,
            RoleRepository roleRepository,
            RbacSyncProperties rbacSyncProperties,
            GatewayRbacCacheInvalidationClient gatewayRbacCacheInvalidationClient
    ) {
        this.rbacPolicyRepository = rbacPolicyRepository;
        this.roleRepository = roleRepository;
        this.rbacSyncProperties = rbacSyncProperties;
        this.gatewayRbacCacheInvalidationClient = gatewayRbacCacheInvalidationClient;
    }

    /**
     * Retrieves read admin policy for `RbacPolicyService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.service`.
     * @return result produced by readAdminPolicy.
     */
    @Transactional(readOnly = true)
    public RbacPolicyResponse readAdminPolicy() {
        LOGGER.info("CODEx_ENTRY_LOG: Entering RbacPolicyService#readAdminPolicy");
        LOGGER.debug("CODEx_ENTRY_LOG: Entering RbacPolicyService#readAdminPolicy with debug context");
        RbacPolicy policy = loadPolicyRequired();
        return toAdminResponse(policy);
    }

    /**
     * Updates update policy for `RbacPolicyService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.service`.
     * @param request input consumed by updatePolicy.
     * @return result produced by updatePolicy.
     */
    @Transactional
    public RbacPolicyResponse updatePolicy(RbacPolicyUpdateRequest request) {
        RbacPolicy policy = loadOrCreatePolicy();
        String normalizedHierarchy = normalizeText(request.roleHierarchy());
        if (normalizedHierarchy.isBlank()) {
            throw new IllegalArgumentException("Role hierarchy cannot be blank");
        }

        String normalizedDecision = normalizeDecision(request.defaultDecision());
        List<RbacPolicyRule> rules = buildRules(request.rules());

        // Remove existing rows first to avoid unique key conflicts on (policy_id, sort_order)
        // when JPA issues inserts before orphan deletes in a single flush cycle.
        policy.replaceRules(List.of());
        rbacPolicyRepository.saveAndFlush(policy);

        policy.setRoleHierarchy(normalizedHierarchy);
        policy.setDefaultDecision(normalizedDecision);
        policy.replaceRules(rules);
        policy.bumpVersion();

        RbacPolicy saved = rbacPolicyRepository.saveAndFlush(policy);
        gatewayRbacCacheInvalidationClient.invalidate();
        return toAdminResponse(saved);
    }

    /**
     * Retrieves read gateway policy for `RbacPolicyService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.service`.
     * @return result produced by readGatewayPolicy.
     */
    @Transactional(readOnly = true)
    public GatewayRbacPolicyResponse readGatewayPolicy() {
        RbacPolicy policy = loadPolicyRequired();
        return toGatewayResponse(policy);
    }

    /**
     * Retrieves load policy required for `RbacPolicyService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.service`.
     * @return result produced by loadPolicyRequired.
     */
    private RbacPolicy loadPolicyRequired() {
        return rbacPolicyRepository.findByPolicyKey(rbacSyncProperties.getPolicyKey())
                .orElseThrow(() -> new IllegalStateException("RBAC policy not configured: " + rbacSyncProperties.getPolicyKey()));
    }

    /**
     * Retrieves load or create policy for `RbacPolicyService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.service`.
     * @return result produced by loadOrCreatePolicy.
     */
    private RbacPolicy loadOrCreatePolicy() {
        return rbacPolicyRepository.findByPolicyKey(rbacSyncProperties.getPolicyKey())
                .orElseGet(this::createDefaultPolicy);
    }

    /**
     * Creates create default policy for `RbacPolicyService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.service`.
     * @return result produced by createDefaultPolicy.
     */
    private RbacPolicy createDefaultPolicy() {
        RbacPolicy policy = new RbacPolicy(
                UUID.randomUUID(),
                rbacSyncProperties.getPolicyKey(),
                "ROLE_SYSTEM_ADMIN > ROLE_USER",
                "DENY",
                1
        );
        return rbacPolicyRepository.save(policy);
    }

    /**
     * Creates build rules for `RbacPolicyService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.service`.
     * @param requests input consumed by buildRules.
     * @return result produced by buildRules.
     */
    private List<RbacPolicyRule> buildRules(List<RbacRuleRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("At least one RBAC rule is required");
        }

        List<RbacPolicyRule> rules = new ArrayList<>();
        Set<String> rolesToValidate = new LinkedHashSet<>();

        for (int i = 0; i < requests.size(); i++) {
            RbacRuleRequest request = requests.get(i);
            String name = normalizeText(request.name());
            String pathPattern = normalizeText(request.pathPattern());
            String effect = normalizeDecision(request.effect());
            List<String> methods = normalizeMethods(request.methods());
            List<String> requiredRoles = request.allowAnonymous()
                    ? List.of()
                    : normalizeRoles(request.requiredRoles());

            if (name.isBlank()) {
                throw new IllegalArgumentException("Rule name cannot be blank");
            }
            if (pathPattern.isBlank()) {
                throw new IllegalArgumentException("Rule path pattern cannot be blank");
            }

            rolesToValidate.addAll(requiredRoles);

            rules.add(new RbacPolicyRule(
                    UUID.randomUUID(),
                    i,
                    name,
                    String.join(",", methods),
                    pathPattern,
                    effect,
                    request.allowAnonymous(),
                    requiredRoles.isEmpty() ? null : String.join(",", requiredRoles)
            ));
        }

        if (!rolesToValidate.isEmpty()) {
            Set<String> existingRoles = roleRepository.findByNameIn(rolesToValidate).stream()
                    .map(role -> role.getName().toUpperCase(Locale.ROOT))
                    .collect(Collectors.toSet());
            List<String> missingRoles = rolesToValidate.stream()
                    .filter(role -> !existingRoles.contains(role))
                    .toList();
            if (!missingRoles.isEmpty()) {
                throw new IllegalArgumentException("Unknown roles in RBAC policy: " + String.join(", ", missingRoles));
            }
        }

        return rules;
    }

    /**
     * Executes normalize decision for `RbacPolicyService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.service`.
     * @param value input consumed by normalizeDecision.
     * @return result produced by normalizeDecision.
     */
    private String normalizeDecision(String value) {
        String normalized = normalizeText(value).toUpperCase(Locale.ROOT);
        if (!VALID_DECISIONS.contains(normalized)) {
            throw new IllegalArgumentException("Decision must be one of: ALLOW, DENY");
        }
        return normalized;
    }

    /**
     * Executes normalize methods for `RbacPolicyService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.service`.
     * @param methods input consumed by normalizeMethods.
     * @return result produced by normalizeMethods.
     */
    private List<String> normalizeMethods(List<String> methods) {
        if (methods == null || methods.isEmpty()) {
            throw new IllegalArgumentException("Rule methods cannot be empty");
        }
        List<String> normalized = methods.stream()
                .map(this::normalizeText)
                .map(value -> value.toUpperCase(Locale.ROOT))
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Rule methods cannot be empty");
        }
        return normalized;
    }

    /**
     * Executes normalize roles for `RbacPolicyService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.service`.
     * @param roles input consumed by normalizeRoles.
     * @return result produced by normalizeRoles.
     */
    private List<String> normalizeRoles(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return List.of();
        }
        return roles.stream()
                .map(this::normalizeText)
                .map(value -> value.toUpperCase(Locale.ROOT))
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    /**
     * Executes normalize text for `RbacPolicyService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.service`.
     * @param value input consumed by normalizeText.
     * @return result produced by normalizeText.
     */
    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * Executes to admin response for `RbacPolicyService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.service`.
     * @param policy input consumed by toAdminResponse.
     * @return result produced by toAdminResponse.
     */
    private RbacPolicyResponse toAdminResponse(RbacPolicy policy) {
        List<String> availableRoles = roleRepository.findAll().stream()
                .map(role -> role.getName().toUpperCase(Locale.ROOT))
                .sorted()
                .toList();

        List<RbacRuleResponse> rules = policy.getRules().stream()
                .map(rule -> new RbacRuleResponse(
                        rule.getId(),
                        rule.getSortOrder(),
                        rule.getName(),
                        splitCsv(rule.getMethods()),
                        rule.getPathPattern(),
                        rule.getEffect(),
                        rule.isAllowAnonymous(),
                        splitCsv(rule.getRequiredRoles())
                ))
                .toList();

        return new RbacPolicyResponse(
                policy.getPolicyKey(),
                policy.getRoleHierarchy(),
                policy.getDefaultDecision(),
                policy.getVersion(),
                policy.getUpdatedAt(),
                availableRoles,
                rules
        );
    }

    /**
     * Executes to gateway response for `RbacPolicyService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.service`.
     * @param policy input consumed by toGatewayResponse.
     * @return result produced by toGatewayResponse.
     */
    private GatewayRbacPolicyResponse toGatewayResponse(RbacPolicy policy) {
        List<GatewayRbacRuleResponse> rules = policy.getRules().stream()
                .map(rule -> new GatewayRbacRuleResponse(
                        rule.getName(),
                        splitCsv(rule.getMethods()),
                        rule.getPathPattern(),
                        rule.getEffect(),
                        rule.isAllowAnonymous(),
                        splitCsv(rule.getRequiredRoles())
                ))
                .toList();

        return new GatewayRbacPolicyResponse(
                policy.getPolicyKey(),
                policy.getRoleHierarchy(),
                policy.getDefaultDecision(),
                policy.getVersion(),
                rules
        );
    }

    /**
     * Executes split csv for `RbacPolicyService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.service`.
     * @param value input consumed by splitCsv.
     * @return result produced by splitCsv.
     */
    private List<String> splitCsv(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return List.of(value.split(",")).stream()
                .map(this::normalizeText)
                .filter(part -> !part.isBlank())
                .toList();
    }
}
