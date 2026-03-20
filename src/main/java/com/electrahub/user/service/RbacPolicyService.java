package com.electrahub.user.service;

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

    @Transactional(readOnly = true)
    public RbacPolicyResponse readAdminPolicy() {
        RbacPolicy policy = loadPolicyRequired();
        return toAdminResponse(policy);
    }

    @Transactional
    public RbacPolicyResponse updatePolicy(RbacPolicyUpdateRequest request) {
        RbacPolicy policy = loadOrCreatePolicy();
        String normalizedHierarchy = normalizeText(request.roleHierarchy());
        if (normalizedHierarchy.isBlank()) {
            throw new IllegalArgumentException("Role hierarchy cannot be blank");
        }

        String normalizedDecision = normalizeDecision(request.defaultDecision());
        List<RbacPolicyRule> rules = buildRules(request.rules());

        policy.setRoleHierarchy(normalizedHierarchy);
        policy.setDefaultDecision(normalizedDecision);
        policy.replaceRules(rules);
        policy.bumpVersion();

        RbacPolicy saved = rbacPolicyRepository.save(policy);
        gatewayRbacCacheInvalidationClient.invalidate();
        return toAdminResponse(saved);
    }

    @Transactional(readOnly = true)
    public GatewayRbacPolicyResponse readGatewayPolicy() {
        RbacPolicy policy = loadPolicyRequired();
        return toGatewayResponse(policy);
    }

    private RbacPolicy loadPolicyRequired() {
        return rbacPolicyRepository.findByPolicyKey(rbacSyncProperties.getPolicyKey())
                .orElseThrow(() -> new IllegalStateException("RBAC policy not configured: " + rbacSyncProperties.getPolicyKey()));
    }

    private RbacPolicy loadOrCreatePolicy() {
        return rbacPolicyRepository.findByPolicyKey(rbacSyncProperties.getPolicyKey())
                .orElseGet(this::createDefaultPolicy);
    }

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

    private String normalizeDecision(String value) {
        String normalized = normalizeText(value).toUpperCase(Locale.ROOT);
        if (!VALID_DECISIONS.contains(normalized)) {
            throw new IllegalArgumentException("Decision must be one of: ALLOW, DENY");
        }
        return normalized;
    }

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

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

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
