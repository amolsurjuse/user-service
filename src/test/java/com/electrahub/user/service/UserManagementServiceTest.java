package com.electrahub.user.service;

import com.electrahub.user.api.dto.UserCountResponse;
import com.electrahub.user.api.dto.UserSearchResponse;
import com.electrahub.user.domain.Role;
import com.electrahub.user.domain.User;
import com.electrahub.user.repository.AddressRepository;
import com.electrahub.user.repository.CountryRepository;
import com.electrahub.user.repository.RoleRepository;
import com.electrahub.user.repository.UserRepository;
import com.electrahub.user.security.AuthenticatedUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private CountryRepository countryRepository;

    @Mock
    private PaymentProvisioningClient paymentProvisioningClient;

    @InjectMocks
    private UserManagementService userManagementService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void searchReturnsRegularUsersForSystemAdmin() {
        setCurrentUser(UUID.randomUUID(), "sysadmin.dev@electrahub.com", "SYSTEM_ADMIN", "USER");
        User regularUser = createUser("driver.user.dev@electrahub.com", "USER");

        when(userRepository.searchRegularUsers("", PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(regularUser), PageRequest.of(0, 10), 1));
        when(userRepository.countSearchRegularUsers("")).thenReturn(1L);

        UserSearchResponse response = userManagementService.search("", 10, 0);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().email()).isEqualTo("driver.user.dev@electrahub.com");
        assertThat(response.total()).isEqualTo(1);
        verify(userRepository).searchRegularUsers("", PageRequest.of(0, 10));
        verify(userRepository).countSearchRegularUsers("");
        verify(userRepository, never()).searchSystemAdmins("", PageRequest.of(0, 10));
    }

    @Test
    void searchAdminUsersReturnsSystemAdminsOnly() {
        User adminUser = createUser("network.operator.dev@electrahub.com", "SYSTEM_ADMIN", "USER");

        when(userRepository.searchSystemAdmins("network", PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(adminUser), PageRequest.of(0, 10), 1));
        when(userRepository.countSearchSystemAdmins("network")).thenReturn(1L);

        var response = userManagementService.searchAdminUsers("network", 10, 0);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().email()).isEqualTo("network.operator.dev@electrahub.com");
        assertThat(response.items().getFirst().roles()).contains("SYSTEM_ADMIN");
        assertThat(response.total()).isEqualTo(1);
        verify(userRepository).searchSystemAdmins("network", PageRequest.of(0, 10));
        verify(userRepository).countSearchSystemAdmins("network");
    }

    @Test
    void countReturnsRegularUserCountForSystemAdmin() {
        setCurrentUser(UUID.randomUUID(), "sysadmin.dev@electrahub.com", "SYSTEM_ADMIN", "USER");
        when(userRepository.countSearchRegularUsers("driver")).thenReturn(3L);

        UserCountResponse response = userManagementService.count("driver");

        assertThat(response.count()).isEqualTo(3);
        verify(userRepository).countSearchRegularUsers("driver");
    }

    @Test
    void searchReturnsCurrentUserForPlainUser() {
        UUID userId = UUID.randomUUID();
        setCurrentUser(userId, "driver.user.dev@electrahub.com", "USER");
        User currentUser = createUser(userId, "driver.user.dev@electrahub.com", "USER");

        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(currentUser));

        UserSearchResponse response = userManagementService.search("driver", 10, 0);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().email()).isEqualTo("driver.user.dev@electrahub.com");
        verify(userRepository, never()).searchRegularUsers("driver", PageRequest.of(0, 10));
        verify(userRepository, never()).searchSystemAdmins("driver", PageRequest.of(0, 10));
    }

    private void setCurrentUser(UUID userId, String email, String... roles) {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(userId, email, List.of(roles));
        var authentication = new UsernamePasswordAuthenticationToken(authenticatedUser, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private User createUser(String email, String... roles) {
        return createUser(UUID.randomUUID(), email, roles);
    }

    private User createUser(UUID userId, String email, String... roles) {
        User user = new User(userId, email, "hash", true, OffsetDateTime.now());
        user.setFirstName("Test");
        user.setLastName("User");
        user.setPhoneNumber("+15550000000");
        for (String roleName : roles) {
            user.addRole(new Role(UUID.randomUUID(), roleName));
        }
        return user;
    }
}
