package com.electrahub.user.service;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.electrahub.user.api.dto.AccountDeletionDecision;
import com.electrahub.user.api.dto.AccountDeletionResponse;
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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserManagementServiceTest.class);


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

    @Mock
    private PaymentAccountStateClient paymentAccountStateClient;

    @Mock
    private SessionActivityClient sessionActivityClient;

    @InjectMocks
    private UserManagementService userManagementService;

    /**
     * Removes clear security context for `UserManagementServiceTest`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.service`.
     */
    @AfterEach
    void clearSecurityContext() {
        LOGGER.info("CODEx_ENTRY_LOG: Entering UserManagementServiceTest#clearSecurityContext");
        LOGGER.debug("CODEx_ENTRY_LOG: Entering UserManagementServiceTest#clearSecurityContext with debug context");
        SecurityContextHolder.clearContext();
    }

    /**
     * Executes search returns regular users for system admin for `UserManagementServiceTest`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.service`.
     */
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

    /**
     * Executes search admin users returns system admins only for `UserManagementServiceTest`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.service`.
     */
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

    /**
     * Executes count returns regular user count for system admin for `UserManagementServiceTest`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.service`.
     */
    @Test
    void countReturnsRegularUserCountForSystemAdmin() {
        setCurrentUser(UUID.randomUUID(), "sysadmin.dev@electrahub.com", "SYSTEM_ADMIN", "USER");
        when(userRepository.countSearchRegularUsers("driver")).thenReturn(3L);

        UserCountResponse response = userManagementService.count("driver");

        assertThat(response.count()).isEqualTo(3);
        verify(userRepository).countSearchRegularUsers("driver");
    }

    /**
     * Executes search returns current user for plain user for `UserManagementServiceTest`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.service`.
     */
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

    @Test
    void requestAccountDeletionBlocksWhenActiveSessionExists() {
        UUID userId = UUID.randomUUID();
        User currentUser = createUser(userId, "driver.user.dev@electrahub.com", "USER");
        setCurrentUser(userId, "driver.user.dev@electrahub.com", "USER");

        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(currentUser));
        when(sessionActivityClient.hasActiveChargingSession(userId.toString())).thenReturn(true);
        when(paymentAccountStateClient.walletBalance(userId.toString())).thenReturn(new BigDecimal("17.25"));

        AccountDeletionResponse response = userManagementService.requestAccountDeletion(userId, false);

        assertThat(response.decision()).isEqualTo(AccountDeletionDecision.ACTIVE_SESSION_IN_PROGRESS);
        assertThat(response.activeCharging()).isTrue();
        assertThat(response.deleted()).isFalse();
        assertThat(currentUser.isPendingDeletion()).isFalse();
        verify(userRepository, never()).save(currentUser);
        verify(userRepository, never()).delete(currentUser);
    }

    @Test
    void requestAccountDeletionMarksPendingWhenWalletBalanceExists() {
        UUID userId = UUID.randomUUID();
        User currentUser = createUser(userId, "driver.user.dev@electrahub.com", "USER");
        setCurrentUser(userId, "driver.user.dev@electrahub.com", "USER");

        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(currentUser));
        when(sessionActivityClient.hasActiveChargingSession(userId.toString())).thenReturn(false);
        when(paymentAccountStateClient.walletBalance(userId.toString())).thenReturn(new BigDecimal("5.00"));
        when(userRepository.save(currentUser)).thenReturn(currentUser);

        AccountDeletionResponse response = userManagementService.requestAccountDeletion(userId, false);

        assertThat(response.decision()).isEqualTo(AccountDeletionDecision.ACCOUNT_MARKED_PENDING_DELETION);
        assertThat(response.pendingDeletion()).isTrue();
        assertThat(currentUser.isPendingDeletion()).isTrue();
        assertThat(currentUser.getDeletionRequestedAt()).isNotNull();
        assertThat(currentUser.isEnabled()).isFalse();
        verify(userRepository).save(currentUser);
    }

    @Test
    void requestAccountDeletionRequiresConfirmationWhenNoBalanceAndNoSession() {
        UUID userId = UUID.randomUUID();
        User currentUser = createUser(userId, "driver.user.dev@electrahub.com", "USER");
        setCurrentUser(userId, "driver.user.dev@electrahub.com", "USER");

        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(currentUser));
        when(sessionActivityClient.hasActiveChargingSession(userId.toString())).thenReturn(false);
        when(paymentAccountStateClient.walletBalance(userId.toString())).thenReturn(BigDecimal.ZERO);

        AccountDeletionResponse response = userManagementService.requestAccountDeletion(userId, false);

        assertThat(response.decision()).isEqualTo(AccountDeletionDecision.CONFIRM_DIRECT_DELETION);
        assertThat(response.directDeletionEligible()).isTrue();
        assertThat(response.deleted()).isFalse();
        verify(userRepository, never()).saveAndFlush(currentUser);
        verify(userRepository, never()).delete(currentUser);
    }

    @Test
    void requestAccountDeletionHardDeletesWhenConfirmedAndNoBalanceAndNoSession() {
        UUID userId = UUID.randomUUID();
        User currentUser = createUser(userId, "driver.user.dev@electrahub.com", "USER");
        setCurrentUser(userId, "driver.user.dev@electrahub.com", "USER");

        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(currentUser));
        when(sessionActivityClient.hasActiveChargingSession(userId.toString())).thenReturn(false);
        when(paymentAccountStateClient.walletBalance(userId.toString())).thenReturn(BigDecimal.ZERO);

        AccountDeletionResponse response = userManagementService.requestAccountDeletion(userId, true);

        assertThat(response.decision()).isEqualTo(AccountDeletionDecision.ACCOUNT_DELETED);
        assertThat(response.deleted()).isTrue();
        verify(userRepository).saveAndFlush(currentUser);
        verify(userRepository).delete(currentUser);
        verify(userRepository).flush();
    }

    /**
     * Updates set current user for `UserManagementServiceTest`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.service`.
     * @param userId input consumed by setCurrentUser.
     * @param email input consumed by setCurrentUser.
     * @param roles input consumed by setCurrentUser.
     */
    private void setCurrentUser(UUID userId, String email, String... roles) {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(userId, email, List.of(roles));
        var authentication = new UsernamePasswordAuthenticationToken(authenticatedUser, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /**
     * Creates create user for `UserManagementServiceTest`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.service`.
     * @param email input consumed by createUser.
     * @param roles input consumed by createUser.
     * @return result produced by createUser.
     */
    private User createUser(String email, String... roles) {
        return createUser(UUID.randomUUID(), email, roles);
    }

    /**
     * Creates create user for `UserManagementServiceTest`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.service`.
     * @param userId input consumed by createUser.
     * @param email input consumed by createUser.
     * @param roles input consumed by createUser.
     * @return result produced by createUser.
     */
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
