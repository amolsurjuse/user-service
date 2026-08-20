package com.electrahub.user.service;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.electrahub.user.api.dto.AddressDto;
import com.electrahub.user.api.dto.RegisterUserRequest;
import com.electrahub.user.api.dto.UserCountResponse;
import com.electrahub.user.api.dto.UserSearchResponse;
import com.electrahub.user.api.dto.UpdateUserProfileRequest;
import com.electrahub.user.api.dto.CountryResponse;
import com.electrahub.user.domain.Role;
import com.electrahub.user.domain.User;
import com.electrahub.user.domain.Country;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
    private UserNotificationOutbox notificationOutbox;

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
        LOGGER.info("Entering UserManagementServiceTest#clearSecurityContext");
        LOGGER.debug("Entering UserManagementServiceTest#clearSecurityContext with debug context");
        SecurityContextHolder.clearContext();
    }

    @Test
    void countriesReturnsOnlySupportedPaymentMarketsInDisplayOrder() {
        Country india = org.mockito.Mockito.mock(Country.class);
        when(india.getIsoCode()).thenReturn("IN");
        when(india.getName()).thenReturn("India");
        when(india.getDialCode()).thenReturn("+91");
        Country canada = org.mockito.Mockito.mock(Country.class);
        when(canada.getIsoCode()).thenReturn("CA");
        Country unitedStates = org.mockito.Mockito.mock(Country.class);
        when(unitedStates.getIsoCode()).thenReturn("US");
        when(unitedStates.getName()).thenReturn("United States");
        when(unitedStates.getDialCode()).thenReturn("+1");
        Country germany = org.mockito.Mockito.mock(Country.class);
        when(germany.getIsoCode()).thenReturn("DE");
        when(germany.getName()).thenReturn("Germany");
        when(germany.getDialCode()).thenReturn("+49");
        Country netherlands = org.mockito.Mockito.mock(Country.class);
        when(netherlands.getIsoCode()).thenReturn("NL");
        when(netherlands.getName()).thenReturn("Netherlands");
        when(netherlands.getDialCode()).thenReturn("+31");
        when(countryRepository.findByEnabledTrueOrderByNameAsc())
                .thenReturn(List.of(canada, netherlands, india, unitedStates, germany));

        List<CountryResponse> countries = userManagementService.countries();

        assertThat(countries).containsExactly(
                new CountryResponse("US", "United States", "+1"),
                new CountryResponse("IN", "India", "+91"),
                new CountryResponse("DE", "Germany", "+49"),
                new CountryResponse("NL", "Netherlands", "+31")
        );
    }

    @Test
    void registerRejectsCountryOutsideSupportedPaymentMarkets() {
        RegisterUserRequest request = new RegisterUserRequest(
                "driver@example.com",
                "password123",
                "Avery",
                "Driver",
                "+15551234567",
                new AddressDto("100 Main St", "Toronto", "ON", "M5V 2T6", "CA")
        );
        when(userRepository.existsByEmail("driver@example.com")).thenReturn(false);

        assertThatThrownBy(() -> userManagementService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Country not available");
        verify(countryRepository, never()).findByIsoCodeAndEnabledTrue("CA");
    }

    /**
     * Executes search returns regular users for system admin for `UserManagementServiceTest`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.service`.
     */
    @Test
    void getProfileInfersNonNullCountryFromInternationalPhoneForLegacyUser() {
        UUID userId = UUID.randomUUID();
        setCurrentUser(userId, "driver@example.com", "USER");
        User user = createUser("driver@example.com", "USER");
        user.setPhoneNumber("+919970238174");
        Country india = org.mockito.Mockito.mock(Country.class);
        when(india.getIsoCode()).thenReturn("IN");
        when(india.getName()).thenReturn("India");
        when(india.getDialCode()).thenReturn("+91");

        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(user));
        when(countryRepository.findByEnabledTrueOrderByNameAsc()).thenReturn(List.of(india));

        var profile = userManagementService.getProfile(userId);

        assertThat(profile.countryCode()).isEqualTo("IN");
        assertThat(profile.countryName()).isEqualTo("India");
        assertThat(profile.countryDialCode()).isEqualTo("+91");
    }

    @Test
    void getProfileAlwaysReturnsDefaultCountryWhenCountryCannotBeInferred() {
        UUID userId = UUID.randomUUID();
        setCurrentUser(userId, "driver@example.com", "USER");
        User user = createUser("driver@example.com", "USER");
        user.setPhoneNumber(null);

        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(user));
        when(countryRepository.findByIsoCodeAndEnabledTrue("US")).thenReturn(java.util.Optional.empty());

        var profile = userManagementService.getProfile(userId);

        assertThat(profile.countryCode()).isEqualTo("US");
        assertThat(profile.countryName()).isEqualTo("United States");
        assertThat(profile.countryDialCode()).isEqualTo("+1");
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
        verify(userRepository, never()).searchAdministrativeUsers("", PageRequest.of(0, 10));
    }

    /**
     * Executes search admin users returns hierarchy administrators for `UserManagementServiceTest`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.service`.
     */
    @Test
    void searchAdminUsersReturnsHierarchyAdministrators() {
        User adminUser = createUser("network.operator.dev@electrahub.com", "NETWORK");

        when(userRepository.searchAdministrativeUsers("network", PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(adminUser), PageRequest.of(0, 10), 1));
        when(userRepository.countSearchAdministrativeUsers("network")).thenReturn(1L);

        var response = userManagementService.searchAdminUsers("network", 10, 0);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().email()).isEqualTo("network.operator.dev@electrahub.com");
        assertThat(response.items().getFirst().roles()).contains("NETWORK");
        assertThat(response.total()).isEqualTo(1);
        verify(userRepository).searchAdministrativeUsers("network", PageRequest.of(0, 10));
        verify(userRepository).countSearchAdministrativeUsers("network");
    }

    @Test
    void resolveAdminUsersLoadsRequestedDirectoryEntriesInOneRepositoryCall() {
        UUID driverId = UUID.randomUUID();
        UUID missingId = UUID.randomUUID();
        User driver = createUser(driverId, "driver.user.dev@electrahub.com", "CUSTOMER", "USER");

        when(userRepository.findByIdIn(List.of(driverId, missingId))).thenReturn(List.of(driver));

        var response = userManagementService.resolveAdminUsers(List.of(driverId, missingId, driverId));

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().userId()).isEqualTo(driverId);
        assertThat(response.getFirst().email()).isEqualTo("driver.user.dev@electrahub.com");
        assertThat(response.getFirst().roles()).containsExactly("CUSTOMER", "USER");
        verify(userRepository).findByIdIn(List.of(driverId, missingId));
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
        verify(userRepository, never()).searchAdministrativeUsers("driver", PageRequest.of(0, 10));
    }

    @Test
    void searchRejectsReadOnlyAdminBeforeReadingUsers() {
        setCurrentUser(UUID.randomUUID(), "readonly.admin@electrahub.com", "ADMIN_READ_ONLY", "USER");

        assertThatThrownBy(() -> userManagementService.search("", 10, 0))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Not authorized to access user management.");

        verifyNoInteractions(userRepository);
    }

    @Test
    void countRejectsReadOnlyAdminBeforeReadingUsers() {
        setCurrentUser(UUID.randomUUID(), "readonly.admin@electrahub.com", "ADMIN_READ_ONLY", "USER");

        assertThatThrownBy(() -> userManagementService.count(""))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Not authorized to access user management.");

        verifyNoInteractions(userRepository);
    }

    @Test
    void updateProfileQueuesAnInboxEventWithoutProfileValues() {
        UUID userId = UUID.randomUUID();
        User user = createUser(userId, "driver.user.dev@electrahub.com", "USER");
        setCurrentUser(userId, user.getEmail(), "USER");
        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(user));

        userManagementService.updateProfile(userId, new UpdateUserProfileRequest("Updated", "Driver", null));

        verify(notificationOutbox).enqueue(
                eq("USER_PROFILE_UPDATED"),
                eq(userId),
                argThat(payload -> payload.size() == 1
                        && payload.containsKey("changedFields")
                        && !payload.toString().contains("Updated")
                        && !payload.toString().contains("Driver"))
        );
    }

    @Test
    void updateProfilePreservesBillingIdentityWhenLegacyClientOmitsFields() {
        UUID userId = UUID.randomUUID();
        User user = createUser(userId, "driver.user.dev@electrahub.com", "USER");
        user.setBillingLegalName("Example Fleet LLC");
        user.setTaxRegistrationNumber("US-TAX-123");
        setCurrentUser(userId, user.getEmail(), "USER");
        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(user));

        var response = userManagementService.updateProfile(
                userId,
                new UpdateUserProfileRequest("Updated", "Driver", null)
        );

        assertThat(response.billingLegalName()).isEqualTo("Example Fleet LLC");
        assertThat(response.taxRegistrationNumber()).isEqualTo("US-TAX-123");
    }

    @Test
    void updateProfileUpdatesAndClearsOptionalBillingIdentity() {
        UUID userId = UUID.randomUUID();
        User user = createUser(userId, "driver.user.dev@electrahub.com", "USER");
        setCurrentUser(userId, user.getEmail(), "USER");
        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(user));

        var updated = userManagementService.updateProfile(
                userId,
                new UpdateUserProfileRequest("Updated", "Driver", null, "Example Fleet LLC", "IN27ABCDE1234F1Z5")
        );

        assertThat(updated.billingLegalName()).isEqualTo("Example Fleet LLC");
        assertThat(updated.taxRegistrationNumber()).isEqualTo("IN27ABCDE1234F1Z5");

        var cleared = userManagementService.updateProfile(
                userId,
                new UpdateUserProfileRequest("Updated", "Driver", null, "", "")
        );
        assertThat(cleared.billingLegalName()).isNull();
        assertThat(cleared.taxRegistrationNumber()).isNull();
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
