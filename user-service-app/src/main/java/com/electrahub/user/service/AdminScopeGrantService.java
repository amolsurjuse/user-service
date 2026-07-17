package com.electrahub.user.service;

import com.electrahub.user.api.dto.AdminAccessContextResponse;
import com.electrahub.user.api.dto.AdminScopeGrantRequest;
import com.electrahub.user.api.dto.AdminScopeGrantResponse;
import com.electrahub.user.api.error.NotFoundException;
import com.electrahub.user.api.error.UnauthorizedException;
import com.electrahub.user.domain.AdminScopeGrant;
import com.electrahub.user.domain.AdminScopeType;
import com.electrahub.user.domain.Role;
import com.electrahub.user.domain.User;
import com.electrahub.user.repository.AdminScopeGrantRepository;
import com.electrahub.user.repository.RoleRepository;
import com.electrahub.user.repository.UserRepository;
import com.electrahub.user.security.AuthenticatedUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminScopeGrantService {

    private static final Set<String> MANAGED_SCOPE_ROLES = Set.of("ENTERPRISE", "NETWORK", "LOCATION");

    private final AdminScopeGrantRepository grantRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public AdminScopeGrantService(
            AdminScopeGrantRepository grantRepository,
            UserRepository userRepository,
            RoleRepository roleRepository
    ) {
        this.grantRepository = grantRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Transactional(readOnly = true)
    public AdminAccessContextResponse currentAccessContext() {
        AuthenticatedUser actor = currentUser();
        boolean systemAdmin = actor.hasRole("SYSTEM_ADMIN");
        List<AdminScopeGrantResponse> grants = systemAdmin
                ? List.of()
                : toResponses(grantRepository.findByUserIdOrderByScopeTypeAscScopeIdAsc(actor.userId()));
        return new AdminAccessContextResponse(actor.userId(), systemAdmin, grants);
    }

    @Transactional(readOnly = true)
    public List<AdminScopeGrantResponse> listGrants(UUID userId) {
        requireSystemAdmin();
        requireUser(userId);
        return toResponses(grantRepository.findByUserIdOrderByScopeTypeAscScopeIdAsc(userId));
    }

    @Transactional
    public List<AdminScopeGrantResponse> replaceGrants(UUID userId, Collection<AdminScopeGrantRequest> requestedGrants) {
        AuthenticatedUser actor = requireSystemAdmin();
        User user = requireUser(userId);
        if (actor.userId().equals(userId) && !actor.hasRole("SYSTEM_ADMIN")) {
            throw new AccessDeniedException("You cannot change your own administrative scope.");
        }

        List<AdminScopeGrantRequest> normalized = normalize(requestedGrants);
        grantRepository.deleteByUserId(userId);
        grantRepository.flush();

        OffsetDateTime now = OffsetDateTime.now();
        List<AdminScopeGrant> grants = normalized.stream()
                .map(request -> new AdminScopeGrant(
                        UUID.randomUUID(),
                        user,
                        request.scopeType(),
                        request.scopeId().trim().toUpperCase(Locale.ROOT),
                        request.accessLevel(),
                        actor.userId(),
                        now
                ))
                .toList();
        grantRepository.saveAll(grants);
        synchronizeScopeRoles(user, grants);
        userRepository.save(user);

        return toResponses(grants);
    }

    private List<AdminScopeGrantRequest> normalize(Collection<AdminScopeGrantRequest> requestedGrants) {
        if (requestedGrants == null || requestedGrants.isEmpty()) {
            return List.of();
        }
        if (requestedGrants.size() > 250) {
            throw new IllegalArgumentException("A user can have at most 250 administrative scope grants.");
        }

        Map<String, AdminScopeGrantRequest> unique = requestedGrants.stream()
                .map(this::normalizeGrant)
                .collect(Collectors.toMap(
                        request -> request.scopeType().name() + ":" + request.scopeId(),
                        Function.identity(),
                        (left, right) -> left.accessLevel().ordinal() >= right.accessLevel().ordinal() ? left : right
                ));
        return unique.values().stream()
                .sorted((left, right) -> {
                    int type = left.scopeType().compareTo(right.scopeType());
                    return type != 0 ? type : left.scopeId().compareTo(right.scopeId());
                })
                .toList();
    }

    private AdminScopeGrantRequest normalizeGrant(AdminScopeGrantRequest request) {
        if (request == null || request.scopeType() == null || request.accessLevel() == null) {
            throw new IllegalArgumentException("Scope type, scope ID, and access level are required.");
        }
        String scopeId = request.scopeId() == null ? "" : request.scopeId().trim().toUpperCase(Locale.ROOT);
        if (scopeId.isBlank()) {
            throw new IllegalArgumentException("Scope ID is required.");
        }
        return new AdminScopeGrantRequest(request.scopeType(), scopeId, request.accessLevel());
    }

    private void synchronizeScopeRoles(User user, List<AdminScopeGrant> grants) {
        Set<String> desired = grants.stream()
                .map(grant -> grant.getScopeType().name())
                .collect(Collectors.toSet());

        user.getRoles().removeIf(role -> MANAGED_SCOPE_ROLES.contains(role.getName()) && !desired.contains(role.getName()));
        Set<String> existing = user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
        roleRepository.findByNameIn(desired).forEach(role -> {
            if (!existing.contains(role.getName())) {
                user.addRole(role);
            }
        });
    }

    private List<AdminScopeGrantResponse> toResponses(List<AdminScopeGrant> grants) {
        return grants.stream()
                .map(grant -> new AdminScopeGrantResponse(
                        grant.getId(),
                        grant.getScopeType(),
                        grant.getScopeId(),
                        grant.getAccessLevel(),
                        grant.getCreatedAt(),
                        grant.getUpdatedAt()
                ))
                .toList();
    }

    private User requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
    }

    private AuthenticatedUser requireSystemAdmin() {
        AuthenticatedUser actor = currentUser();
        if (!actor.hasRole("SYSTEM_ADMIN")) {
            throw new AccessDeniedException("System administrator access is required.");
        }
        return actor;
    }

    private AuthenticatedUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new UnauthorizedException("Authentication required");
        }
        return user;
    }
}
