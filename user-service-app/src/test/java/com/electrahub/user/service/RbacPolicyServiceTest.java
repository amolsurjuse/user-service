package com.electrahub.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.electrahub.user.api.dto.RbacPolicyResponse;
import com.electrahub.user.api.dto.RbacPolicyUpdateRequest;
import com.electrahub.user.api.dto.RbacRuleRequest;
import com.electrahub.user.config.RbacSyncProperties;
import com.electrahub.user.domain.RbacPolicy;
import com.electrahub.user.domain.Role;
import com.electrahub.user.repository.RbacPolicyRepository;
import com.electrahub.user.repository.RoleRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RbacPolicyServiceTest {

    @Mock
    private RbacPolicyRepository policyRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private GatewayRbacCacheInvalidationClient cacheInvalidationClient;

    @Test
    void replacesLegacyPaymentGatewayRuleWithTheSystemManagedControlPlaneRoute() {
        RbacSyncProperties properties = new RbacSyncProperties();
        RbacPolicy policy = new RbacPolicy(
                UUID.randomUUID(),
                properties.getPolicyKey(),
                "ROLE_SYSTEM_ADMIN > ROLE_USER",
                "DENY",
                1
        );
        Role systemAdmin = new Role(UUID.randomUUID(), "SYSTEM_ADMIN");

        when(policyRepository.findByPolicyKey(properties.getPolicyKey())).thenReturn(Optional.of(policy));
        when(policyRepository.saveAndFlush(any(RbacPolicy.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(roleRepository.findByNameIn(anySet())).thenReturn(List.of(systemAdmin));
        when(roleRepository.findAll()).thenReturn(List.of(systemAdmin));

        RbacPolicyService service = new RbacPolicyService(
                policyRepository,
                roleRepository,
                properties,
                cacheInvalidationClient
        );
        RbacPolicyUpdateRequest request = new RbacPolicyUpdateRequest(
                "ROLE_SYSTEM_ADMIN > ROLE_USER",
                "DENY",
                List.of(
                        new RbacRuleRequest(
                                "payment-gateway-read",
                                List.of("GET"),
                                "/gateway/admin/configuration/**",
                                "ALLOW",
                                false,
                                List.of("SYSTEM_ADMIN")
                        ),
                        new RbacRuleRequest(
                                "payment-gateway-provider-webhooks",
                                List.of("GET"),
                                "/payment-gateway/api/v1/gateway/webhooks/*",
                                "DENY",
                                false,
                                List.of("SYSTEM_ADMIN")
                        )
                )
        );

        RbacPolicyResponse response = service.updatePolicy(request);

        assertThat(response.rules()).hasSize(2);
        assertThat(response.rules().get(0)).satisfies(rule -> {
            assertThat(rule.name()).isEqualTo("payment-gateway-admin");
            assertThat(rule.methods()).containsExactly("*");
            assertThat(rule.pathPattern()).isEqualTo("/payment-gateway/api/v1/gateway/admin/**");
            assertThat(rule.effect()).isEqualTo("ALLOW");
            assertThat(rule.allowAnonymous()).isFalse();
            assertThat(rule.requiredRoles()).containsExactly("SYSTEM_ADMIN");
        });
        assertThat(response.rules().get(1)).satisfies(rule -> {
            assertThat(rule.name()).isEqualTo("payment-gateway-provider-webhooks");
            assertThat(rule.methods()).containsExactly("POST");
            assertThat(rule.pathPattern()).isEqualTo("/payment-gateway/api/v1/gateway/webhooks/*");
            assertThat(rule.effect()).isEqualTo("ALLOW");
            assertThat(rule.allowAnonymous()).isTrue();
            assertThat(rule.requiredRoles()).isEmpty();
        });
        verify(cacheInvalidationClient).invalidate();
    }
}
