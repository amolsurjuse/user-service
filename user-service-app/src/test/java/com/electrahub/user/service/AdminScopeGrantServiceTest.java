package com.electrahub.user.service;

import com.electrahub.user.api.dto.AdminProvisionScopedUserRequest;
import com.electrahub.user.api.dto.AdminScopeGrantRequest;
import com.electrahub.user.domain.AdminScopeAccess;
import com.electrahub.user.domain.AdminScopeType;
import com.electrahub.user.domain.Role;
import com.electrahub.user.domain.User;
import com.electrahub.user.repository.AdminScopeGrantRepository;
import com.electrahub.user.repository.RoleRepository;
import com.electrahub.user.repository.UserRepository;
import com.electrahub.user.security.AuthenticatedUser;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminScopeGrantServiceTest {

    @Mock
    private AdminScopeGrantRepository grants;

    @Mock
    private UserRepository users;

    @Mock
    private RoleRepository roles;

    @Mock
    private ApplicationEventPublisher events;

    @InjectMocks
    private AdminScopeGrantService service;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void provisionsAScopedOperatorWithoutDriverRolesOrWalletProvisioning() {
        UUID actorId = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(actorId, "system.admin@electrahub.com", List.of("SYSTEM_ADMIN")),
                null
        ));

        Role networkRole = new Role(UUID.randomUUID(), "NETWORK");
        when(users.existsByEmail("operator@electrahub.com")).thenReturn(false);
        when(roles.findByNameIn(anyCollection())).thenReturn(List.of(networkRole));

        var response = service.provisionScopedUser(new AdminProvisionScopedUserRequest(
                "operator@electrahub.com",
                "a-strong-initial-password",
                "Network",
                "Operator",
                "+15550000002",
                List.of(new AdminScopeGrantRequest(AdminScopeType.NETWORK, "network-001", AdminScopeAccess.OPERATE))
        ));

        ArgumentCaptor<User> savedUsers = ArgumentCaptor.forClass(User.class);
        verify(users, org.mockito.Mockito.atLeastOnce()).save(savedUsers.capture());
        User operator = savedUsers.getAllValues().getFirst();

        assertThat(response.email()).isEqualTo("operator@electrahub.com");
        assertThat(response.grants()).singleElement().satisfies(grant -> {
            assertThat(grant.scopeType()).isEqualTo(AdminScopeType.NETWORK);
            assertThat(grant.scopeId()).isEqualTo("NETWORK-001");
            assertThat(grant.accessLevel()).isEqualTo(AdminScopeAccess.OPERATE);
        });
        assertThat(operator.isEmailVerified()).isTrue();
        assertThat(operator.getTenantId()).isEqualTo("electrahub");
        assertThat(operator.getRoles()).extracting(Role::getName).containsExactly("NETWORK");

        verify(grants).deleteByUserId(operator.getId());
        verify(grants).saveAll(any());
        verify(events).publishEvent(new AdminScopeGrantsChangedEvent(operator.getId()));
    }
}
