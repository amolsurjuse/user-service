package com.electrahub.user.service;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.electrahub.user.api.dto.AddressDto;
import com.electrahub.user.api.dto.AdminResetPasswordRequest;
import com.electrahub.user.api.dto.AdminUpdateUserRequest;
import com.electrahub.user.api.dto.AdminUserDirectoryEntryResponse;
import com.electrahub.user.api.dto.AdminUserDetailResponse;
import com.electrahub.user.api.dto.AdminUserSearchResponse;
import com.electrahub.user.api.dto.AdminUserSummaryResponse;
import com.electrahub.user.api.dto.AuthenticateUserRequest;
import com.electrahub.user.api.dto.CountryResponse;
import com.electrahub.user.api.dto.RegisterUserRequest;
import com.electrahub.user.api.dto.ResetUserPasswordRequest;
import com.electrahub.user.api.dto.UpdateUserProfileRequest;
import com.electrahub.user.api.dto.AccountDeletionRequest;
import com.electrahub.user.api.dto.AccountDeletionResponse;
import com.electrahub.user.api.dto.UserCountResponse;
import com.electrahub.user.api.dto.UserProfileResponse;
import com.electrahub.user.api.dto.BillingProfileResponse;
import com.electrahub.user.api.dto.UserPrincipalResponse;
import com.electrahub.user.api.dto.UserSearchResponse;
import com.electrahub.user.api.dto.UserSummaryResponse;
import com.electrahub.user.api.error.ConflictException;
import com.electrahub.user.api.error.NotFoundException;
import com.electrahub.user.api.error.UnauthorizedException;
import com.electrahub.user.domain.Address;
import com.electrahub.user.domain.Country;
import com.electrahub.user.domain.User;
import com.electrahub.user.repository.AddressRepository;
import com.electrahub.user.repository.CountryRepository;
import com.electrahub.user.repository.RoleRepository;
import com.electrahub.user.repository.UserRepository;
import com.electrahub.user.security.AuthenticatedUser;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.UUID;

@Service
public class UserManagementService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserManagementService.class);
    private static final List<String> SUPPORTED_COUNTRY_CODES = List.of("US", "IN", "DE", "NL");


    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AddressRepository addressRepository;
    private final CountryRepository countryRepository;
    private final PaymentProvisioningClient paymentProvisioningClient;
    private final UserNotificationOutbox notificationOutbox;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserManagementService(UserRepository userRepository,
                                 RoleRepository roleRepository,
                                  AddressRepository addressRepository,
                                  CountryRepository countryRepository,
                                  PaymentProvisioningClient paymentProvisioningClient,
                                  UserNotificationOutbox notificationOutbox) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.addressRepository = addressRepository;
        this.countryRepository = countryRepository;
        this.paymentProvisioningClient = paymentProvisioningClient;
        this.notificationOutbox = notificationOutbox;
    }

    /**
     * Creates register for `UserManagementService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.service`.
     * @param request input consumed by register.
     * @return result produced by register.
     */
    @Transactional
    public UserPrincipalResponse register(RegisterUserRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        LOGGER.info("Registering new user account for email={}", normalizedEmail);
        if (userRepository.existsByEmail(normalizedEmail)) {
            LOGGER.warn("Registration rejected because email is already registered: {}", normalizedEmail);
            throw new ConflictException("Email already registered");
        }

        OffsetDateTime now = OffsetDateTime.now();
        User user = new User(UUID.randomUUID(), normalizedEmail, passwordEncoder.encode(request.password()), true, now);
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setPhoneNumber(request.phoneNumber());

        boolean communityVoting = "COMMUNITY_VOTING".equalsIgnoreCase(request.application());
        Address address = buildAddress(request.address());
        if (!communityVoting && (address == null || address.getCountry() == null)) {
            throw new IllegalArgumentException("A supported registration country is required");
        }
        if (address != null) {
            addressRepository.save(address);
            user.setAddress(address);
        }

        var userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new IllegalStateException("Role USER not seeded"));
        user.addRole(userRole);
        if (communityVoting) {
            var votingRole = roleRepository.findByName("COMMUNITY_VOTING_USER")
                    .orElseThrow(() -> new IllegalStateException("Role COMMUNITY_VOTING_USER not seeded"));
            user.addRole(votingRole);
        } else {
            roleRepository.findByName("CUSTOMER").ifPresent(user::addRole);
        }

        userRepository.save(user);
        LOGGER.info("User persisted with id={} email={} roles={}", user.getId(), user.getEmail(), user.getRoles().stream().map(role -> role.getName()).toList());

        // Keep user creation and wallet provisioning in one business flow.
        if (!communityVoting) {
            paymentProvisioningClient.createWallet(user.getId().toString(), resolveCountryCode(user));
            LOGGER.info("Wallet provisioning requested for userId={} countryCode={}", user.getId(), resolveCountryCode(user));
        }

        return toPrincipal(user);
    }

    /**
     * Executes authenticate for `UserManagementService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.service`.
     * @param request input consumed by authenticate.
     * @return result produced by authenticate.
     */
    @Transactional(readOnly = true)
    public UserPrincipalResponse authenticate(AuthenticateUserRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        LOGGER.info("Authenticating user by email={}", normalizedEmail);
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (!user.isEnabled()) {
            LOGGER.warn("Authentication rejected for disabled account userId={}", user.getId());
            throw new UnauthorizedException("User is disabled");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            LOGGER.warn("Authentication rejected due to credential mismatch for email={}", normalizedEmail);
            throw new UnauthorizedException("Invalid credentials");
        }

        LOGGER.info("Authentication succeeded for userId={}", user.getId());

        return toPrincipal(user);
    }

    /**
     * Retrieves get principal for `UserManagementService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.service`.
     * @param userId input consumed by getPrincipal.
     * @return result produced by getPrincipal.
     */
    @Transactional(readOnly = true)
    public UserPrincipalResponse getPrincipal(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
        return toPrincipal(user);
    }

    @Transactional(readOnly = true)
    public UserPrincipalResponse getPrincipalByEmail(String email) {
        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new NotFoundException("User not found"));
        return toPrincipal(user);
    }

    @Transactional
    public UserPrincipalResponse markEmailVerified(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
        if (!user.isEmailVerified()) {
            user.markEmailVerified();
            LOGGER.info("Email marked verified for userId={}", userId);
        }
        return toPrincipal(user);
    }

    @Transactional(readOnly = true)
    public PhoneVerificationContact getPhoneVerificationContact(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
        return new PhoneVerificationContact(user.getPhoneNumber(), user.isPhoneVerified());
    }

    @Transactional
    public PhoneVerificationContact markPhoneVerified(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
        if (user.getPhoneNumber() == null || user.getPhoneNumber().isBlank()) {
            throw new IllegalArgumentException("A phone number is required");
        }
        user.markPhoneVerified();
        return new PhoneVerificationContact(user.getPhoneNumber(), true);
    }

    public record PhoneVerificationContact(String phoneNumber, boolean phoneVerified) {}

    /**
     * Retrieves get profile for `UserManagementService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.service`.
     * @param userId input consumed by getProfile.
     * @return result produced by getProfile.
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID userId) {
        requireSelfOrSystemAdmin(userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
        return toProfile(user);
    }

    @Transactional(readOnly = true)
    public BillingProfileResponse getBillingProfileInternal(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
        Address address = user.getAddress();
        return new BillingProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                address == null ? null : address.getStreet(),
                address == null ? null : address.getCity(),
                address == null ? null : address.getState(),
                address == null ? null : address.getPostalCode(),
                address == null || address.getCountry() == null ? null : address.getCountry().getIsoCode(),
                user.getBillingLegalName(),
                user.getTaxRegistrationNumber()
        );
    }

    /**
     * Updates update profile for `UserManagementService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.service`.
     * @param userId input consumed by updateProfile.
     * @param request input consumed by updateProfile.
     * @return result produced by updateProfile.
     */
    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UpdateUserProfileRequest request) {
        requireSelfOrSystemAdmin(userId);
        LOGGER.info("Updating profile for userId={} (addressProvided={})", userId, request.address() != null);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        String firstName = normalizeText(request.firstName());
        String lastName = normalizeText(request.lastName());
        List<String> changedFields = new ArrayList<>();
        if (!Objects.equals(user.getFirstName(), firstName)) {
            changedFields.add("firstName");
        }
        if (!Objects.equals(user.getLastName(), lastName)) {
            changedFields.add("lastName");
        }
        if (request.address() != null) {
            changedFields.add("address");
        }
        user.setFirstName(firstName);
        user.setLastName(lastName);
        if (request.billingLegalName() != null) {
            String billingLegalName = normalizeOptionalText(request.billingLegalName());
            if (!Objects.equals(user.getBillingLegalName(), billingLegalName)) {
                changedFields.add("billingLegalName");
            }
            user.setBillingLegalName(billingLegalName);
        }
        if (request.taxRegistrationNumber() != null) {
            String taxRegistrationNumber = normalizeOptionalText(request.taxRegistrationNumber());
            if (!Objects.equals(user.getTaxRegistrationNumber(), taxRegistrationNumber)) {
                changedFields.add("taxRegistrationNumber");
            }
            user.setTaxRegistrationNumber(taxRegistrationNumber);
        }
        applyAddress(user, request.address());
        LOGGER.info("Profile updated for userId={} country={}", userId, resolveCountryCode(user));

        if (!changedFields.isEmpty()) {
            notificationOutbox.enqueue("USER_PROFILE_UPDATED", userId, Map.of("changedFields", List.copyOf(changedFields)));
        }

        return toProfile(user);
    }

    /**
     * Executes search for `UserManagementService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.service`.
     * @param query input consumed by search.
     * @param limit input consumed by search.
     * @param offset input consumed by search.
     * @return result produced by search.
     */
    @Transactional(readOnly = true)
    public UserSearchResponse search(String query, int limit, int offset) {
        AuthenticatedUser actor = currentUser();
        requireUserDirectoryAccess(actor);
        LOGGER.info("User search requested by actorId={} actorRoles={} query='{}' limit={} offset={}",
                actor.userId(), actor.roles(), normalizeQuery(query), limit, offset);
        if (!hasAdminReadAccess(actor)) {
            LOGGER.debug("Search is scoped to caller because actor lacks admin read role: actorId={}", actor.userId());
            return searchCurrentUser(query, limit, offset, actor.userId());
        }

        int safeLimit = Math.max(1, Math.min(limit, 200));
        int safeOffset = Math.max(0, offset);
        int page = safeOffset / safeLimit;
        String normalizedQuery = normalizeQuery(query);

        var pageResult = userRepository.searchRegularUsers(normalizedQuery, PageRequest.of(page, safeLimit));
        var items = pageResult.getContent().stream()
                .map(this::toSummary)
                .toList();

        long total = userRepository.countSearchRegularUsers(normalizedQuery);
        int totalPages = Math.max(pageResult.getTotalPages(), total > 0 ? 1 : 0);
        return new UserSearchResponse(items, total, safeLimit, safeOffset, page, totalPages, pageResult.hasNext(), pageResult.hasPrevious());
    }

    /**
     * Executes count for `UserManagementService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.service`.
     * @param query input consumed by count.
     * @return result produced by count.
     */
    @Transactional(readOnly = true)
    public UserCountResponse count(String query) {
        AuthenticatedUser actor = currentUser();
        requireUserDirectoryAccess(actor);
        if (!hasAdminReadAccess(actor)) {
            long total = matchesUserSearch(
                    userRepository.findById(actor.userId())
                            .orElseThrow(() -> new NotFoundException("User not found: " + actor.userId())),
                    normalizeQuery(query)
            ) ? 1 : 0;
            return new UserCountResponse(normalizeQuery(query), total);
        }

        long count = userRepository.countSearchRegularUsers(normalizeQuery(query));
        return new UserCountResponse(normalizeQuery(query), count);
    }

    /**
     * Executes countries for `UserManagementService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.service`.
     * @return result produced by countries.
     */
    @Transactional(readOnly = true)
    public List<CountryResponse> countries() {
        return countryRepository.findByEnabledTrueOrderByNameAsc().stream()
                .filter(country -> SUPPORTED_COUNTRY_CODES.contains(country.getIsoCode()))
                .sorted(Comparator.comparingInt(country -> SUPPORTED_COUNTRY_CODES.indexOf(country.getIsoCode())))
                .map(c -> new CountryResponse(c.getIsoCode(), c.getName(), c.getDialCode()))
                .toList();
    }

    /**
     * Executes search admin users for `UserManagementService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.service`.
     * @param query input consumed by searchAdminUsers.
     * @param limit input consumed by searchAdminUsers.
     * @param offset input consumed by searchAdminUsers.
     * @return result produced by searchAdminUsers.
     */
    @Transactional(readOnly = true)
    public AdminUserSearchResponse searchAdminUsers(String query, int limit, int offset) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        int safeOffset = Math.max(0, offset);
        int page = safeOffset / safeLimit;
        String normalizedQuery = normalizeQuery(query);

        var pageResult = userRepository.searchAdministrativeUsers(normalizedQuery, PageRequest.of(page, safeLimit));
        var items = pageResult.getContent().stream()
                .map(this::toAdminSummary)
                .toList();
        long total = userRepository.countSearchAdministrativeUsers(normalizedQuery);
        int totalPages = Math.max(pageResult.getTotalPages(), total > 0 ? 1 : 0);

        return new AdminUserSearchResponse(items, total, safeLimit, safeOffset, page, totalPages, pageResult.hasNext(), pageResult.hasPrevious());
    }

    @Transactional(readOnly = true)
    public List<AdminUserDirectoryEntryResponse> resolveAdminUsers(Collection<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }

        List<UUID> uniqueUserIds = userIds.stream().distinct().toList();
        if (uniqueUserIds.size() > 500) {
            throw new IllegalArgumentException("A maximum of 500 users can be resolved at once.");
        }

        var usersById = userRepository.findByIdIn(uniqueUserIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return uniqueUserIds.stream()
                .map(usersById::get)
                .filter(java.util.Objects::nonNull)
                .map(this::toAdminDirectoryEntry)
                .toList();
    }

    /**
     * Retrieves get admin user for `UserManagementService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.service`.
     * @param userId input consumed by getAdminUser.
     * @return result produced by getAdminUser.
     */
    @Transactional(readOnly = true)
    public AdminUserDetailResponse getAdminUser(UUID userId) {
        return toAdminDetail(loadUser(userId));
    }

    /**
     * Updates update admin user for `UserManagementService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.service`.
     * @param userId input consumed by updateAdminUser.
     * @param request input consumed by updateAdminUser.
     * @return result produced by updateAdminUser.
     */
    @Transactional
    public AdminUserDetailResponse updateAdminUser(UUID userId, AdminUpdateUserRequest request) {
        AuthenticatedUser actor = currentUser();
        LOGGER.info("Admin profile update requested by actorId={} targetUserId={} enabled={}", actor.userId(), userId, request.enabled());
        if (actor.userId().equals(userId) && !request.enabled()) {
            throw new AccessDeniedException("You cannot disable your own account.");
        }

        User user = loadUser(userId);
        String firstName = normalizeText(request.firstName());
        String lastName = normalizeText(request.lastName());
        String phoneNumber = normalizeText(request.phoneNumber());
        List<String> changedFields = new ArrayList<>();
        if (!Objects.equals(user.getFirstName(), firstName)) {
            changedFields.add("firstName");
        }
        if (!Objects.equals(user.getLastName(), lastName)) {
            changedFields.add("lastName");
        }
        if (!Objects.equals(user.getPhoneNumber(), phoneNumber)) {
            changedFields.add("phoneNumber");
        }
        if (user.isEnabled() != request.enabled()) {
            changedFields.add("accountStatus");
        }
        if (request.address() != null) {
            changedFields.add("address");
        }

        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPhoneNumber(phoneNumber);
        user.setEnabled(request.enabled());
        applyAddress(user, request.address());

        if (!changedFields.isEmpty()) {
            notificationOutbox.enqueue("USER_PROFILE_UPDATED", userId, Map.of("changedFields", List.copyOf(changedFields)));
        }

        return toAdminDetail(user);
    }

    /**
     * Executes reset password for `UserManagementService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.service`.
     * @param userId input consumed by resetPassword.
     * @param request input consumed by resetPassword.
     */
    @Transactional
    public void resetPassword(UUID userId, AdminResetPasswordRequest request) {
        LOGGER.warn("Administrative password reset initiated for targetUserId={}", userId);
        User user = loadUser(userId);
        user.setPasswordHash(passwordEncoder.encode(request.newPassword().trim()));
        LOGGER.info("Administrative password reset completed for targetUserId={}", userId);
    }

    @Transactional
    public void resetPassword(UUID userId, ResetUserPasswordRequest request) {
        LOGGER.warn("Password reset completed through auth reset flow for targetUserId={}", userId);
        User user = loadUser(userId);
        user.setPasswordHash(passwordEncoder.encode(request.newPassword().trim()));
    }

    /**
     * Removes delete user for `UserManagementService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.service`.
     * @param userId input consumed by deleteUser.
     */
    @Transactional
    public void deleteUser(UUID userId) {
        AuthenticatedUser actor = currentUser();
        LOGGER.warn("Administrative delete requested by actorId={} targetUserId={}", actor.userId(), userId);
        if (actor.userId().equals(userId)) {
            throw new AccessDeniedException("You cannot delete your own account from the admin console.");
        }

        User user = loadUser(userId);
        Address address = user.getAddress();

        user.getRoles().clear();
        user.setAddress(null);
        userRepository.saveAndFlush(user);
        userRepository.delete(user);
        userRepository.flush();

        if (address != null) {
            addressRepository.delete(address);
        }
        LOGGER.info("User deletion completed for targetUserId={}", userId);
    }

    @Transactional
    public AccountDeletionResponse requestAccountDeletion(UUID userId, AccountDeletionRequest request) {
        AuthenticatedUser actor = currentUser();
        if (!actor.userId().equals(userId)) {
            throw new AccessDeniedException("You can delete only your own account.");
        }

        User user = loadUser(userId);
        if (!request.confirmDirectDeletion()) {
            return new AccountDeletionResponse(
                    "CONFIRM_DIRECT_DELETION",
                    "Confirm permanent deletion of your account and profile data.",
                    BigDecimal.ZERO,
                    false,
                    false,
                    false,
                    true,
                    null
            );
        }

        deleteUserRecord(user);
        LOGGER.warn("Self-service account deletion completed for userId={}", userId);
        return new AccountDeletionResponse(
                "ACCOUNT_DELETED",
                "Your account has been permanently deleted.",
                BigDecimal.ZERO,
                false,
                false,
                true,
                true,
                OffsetDateTime.now()
        );
    }

    private void deleteUserRecord(User user) {
        Address address = user.getAddress();
        user.getRoles().clear();
        user.setAddress(null);
        userRepository.saveAndFlush(user);
        userRepository.delete(user);
        userRepository.flush();
        if (address != null) {
            addressRepository.delete(address);
        }
    }

    /**
     * Creates build address for `UserManagementService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.service`.
     * @param dto input consumed by buildAddress.
     * @return result produced by buildAddress.
     */
    private Address buildAddress(AddressDto dto) {
        if (dto == null) {
            return null;
        }

        Country country = resolveCountry(dto.countryIsoCode());

        return new Address(
                UUID.randomUUID(),
                normalizeText(dto.street()),
                normalizeText(dto.city()),
                normalizeText(dto.state()),
                normalizeText(dto.postalCode()),
                country
        );
    }


    /**
     * Processes apply address for `UserManagementService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.service`.
     * @param user input consumed by applyAddress.
     * @param addressDto input consumed by applyAddress.
     */
    private void applyAddress(User user, AddressDto addressDto) {
        if (addressDto == null) {
            return;
        }
        Country country = resolveCountry(addressDto.countryIsoCode());
        if (user.getAddress() == null) {
            Address address = new Address(
                    UUID.randomUUID(),
                    normalizeText(addressDto.street()),
                    normalizeText(addressDto.city()),
                    normalizeText(addressDto.state()),
                    normalizeText(addressDto.postalCode()),
                    country
            );
            addressRepository.save(address);
            user.setAddress(address);
            return;
        }

        Address address = user.getAddress();
        address.setStreet(normalizeText(addressDto.street()));
        address.setCity(normalizeText(addressDto.city()));
        address.setState(normalizeText(addressDto.state()));
        address.setPostalCode(normalizeText(addressDto.postalCode()));
        address.setCountry(country);
    }

    /**
     * Executes normalize email for `UserManagementService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.service`.
     * @param email input consumed by normalizeEmail.
     * @return result produced by normalizeEmail.
     */
    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    /**
     * Executes normalize text for `UserManagementService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.service`.
     * @param value input consumed by normalizeText.
     * @return result produced by normalizeText.
     */
    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeOptionalText(String value) {
        String normalized = normalizeText(value);
        return normalized.isEmpty() ? null : normalized;
    }

    /**
     * Executes normalize query for `UserManagementService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.service`.
     * @param query input consumed by normalizeQuery.
     * @return result produced by normalizeQuery.
     */
    private String normalizeQuery(String query) {
        return query == null ? "" : query.trim();
    }

    /**
     * Executes resolve country for `UserManagementService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.service`.
     * @param countryIsoCode input consumed by resolveCountry.
     * @return result produced by resolveCountry.
     */
    private Country resolveCountry(String countryIsoCode) {
        String code = normalizeText(countryIsoCode).toUpperCase();
        if (code.isBlank()) {
            return null;
        }
        if (!SUPPORTED_COUNTRY_CODES.contains(code)) {
            throw new IllegalArgumentException("Country not available");
        }
        return countryRepository.findByIsoCodeAndEnabledTrue(code)
                .orElseThrow(() -> new IllegalArgumentException("Country not available"));
    }

    /**
     * Executes resolve country code for `UserManagementService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.service`.
     * @param user input consumed by resolveCountryCode.
     * @return result produced by resolveCountryCode.
     */
    private String resolveCountryCode(User user) {
        if (user.getAddress() == null || user.getAddress().getCountry() == null) {
            return "US";
        }
        return user.getAddress().getCountry().getIsoCode();
    }

    /**
     * Executes current user for `UserManagementService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.service`.
     * @return result produced by currentUser.
     */
    private AuthenticatedUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new UnauthorizedException("Authentication required");
        }
        return user;
    }

    /**
     * Executes require self or system admin for `UserManagementService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.service`.
     * @param userId input consumed by requireSelfOrSystemAdmin.
     */
    private void requireSelfOrSystemAdmin(UUID userId) {
        AuthenticatedUser actor = currentUser();
        if (!actor.hasRole("SYSTEM_ADMIN") && !actor.userId().equals(userId)) {
            throw new AccessDeniedException("You are not allowed to access this user.");
        }
    }

    private boolean hasAdminReadAccess(AuthenticatedUser actor) {
        return actor.hasRole("SYSTEM_ADMIN");
    }

    private void requireUserDirectoryAccess(AuthenticatedUser actor) {
        if (actor.hasRole("ADMIN_READ_ONLY") && !actor.hasRole("SYSTEM_ADMIN")) {
            throw new AccessDeniedException("Not authorized to access user management.");
        }
    }

    /**
     * Retrieves load user for `UserManagementService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.service`.
     * @param userId input consumed by loadUser.
     * @return result produced by loadUser.
     */
    private User loadUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
    }

    /**
     * Executes search current user for `UserManagementService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.service`.
     * @param query input consumed by searchCurrentUser.
     * @param limit input consumed by searchCurrentUser.
     * @param offset input consumed by searchCurrentUser.
     * @param userId input consumed by searchCurrentUser.
     * @return result produced by searchCurrentUser.
     */
    private UserSearchResponse searchCurrentUser(String query, int limit, int offset, UUID userId) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        int safeOffset = Math.max(0, offset);
        User user = loadUser(userId);
        boolean matches = matchesUserSearch(user, normalizeQuery(query));
        List<UserSummaryResponse> items = matches && safeOffset == 0 ? List.of(toSummary(user)) : List.of();
        long total = matches ? 1 : 0;
        int currentPage = safeOffset / safeLimit;
        int totalPages = total == 0 ? 0 : 1;
        boolean hasPrevious = currentPage > 0;
        boolean hasNext = false;
        return new UserSearchResponse(items, total, safeLimit, safeOffset, currentPage, totalPages, hasNext, hasPrevious);
    }

    /**
     * Executes matches user search for `UserManagementService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.service`.
     * @param user input consumed by matchesUserSearch.
     * @param normalizedQuery input consumed by matchesUserSearch.
     * @return result produced by matchesUserSearch.
     */
    private boolean matchesUserSearch(User user, String normalizedQuery) {
        if (normalizedQuery.isBlank()) {
            return true;
        }
        String query = normalizedQuery.toLowerCase();
        return contains(user.getEmail(), query)
                || contains(user.getFirstName(), query)
                || contains(user.getLastName(), query)
                || contains(user.getPhoneNumber(), query);
    }

    /**
     * Executes contains for `UserManagementService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.service`.
     * @param source input consumed by contains.
     * @param query input consumed by contains.
     * @return result produced by contains.
     */
    private boolean contains(String source, String query) {
        return source != null && source.toLowerCase().contains(query);
    }

    /**
     * Executes to principal for `UserManagementService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.service`.
     * @param user input consumed by toPrincipal.
     * @return result produced by toPrincipal.
     */
    private UserPrincipalResponse toPrincipal(User user) {
        return new UserPrincipalResponse(
                user.getId(),
                user.getEmail(),
                user.isEnabled(),
                user.isEmailVerified(),
                user.getRoles().stream().map(role -> role.getName()).toList(),
                false,
                user.getTenantId()
        );
    }

    /**
     * Executes to summary for `UserManagementService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.service`.
     * @param user input consumed by toSummary.
     * @return result produced by toSummary.
     */
    private UserSummaryResponse toSummary(User user) {
        return new UserSummaryResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhoneNumber(),
                user.isEnabled(),
                user.getCreatedAt()
        );
    }

    /**
     * Executes to admin summary for `UserManagementService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.service`.
     * @param user input consumed by toAdminSummary.
     * @return result produced by toAdminSummary.
     */
    private AdminUserSummaryResponse toAdminSummary(User user) {
        return new AdminUserSummaryResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhoneNumber(),
                user.isEnabled(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getRoles().stream().map(role -> role.getName()).sorted().toList()
        );
    }

    /**
     * Executes to profile for `UserManagementService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.service`.
     * @param user input consumed by toProfile.
     * @return result produced by toProfile.
     */
    private UserProfileResponse toProfile(User user) {
        String street = null;
        String city = null;
        String state = null;
        String postalCode = null;
        ResolvedCountry resolvedCountry = resolveProfileCountry(user);
        String countryCode = resolvedCountry.code();
        String countryName = resolvedCountry.name();
        String countryDialCode = resolvedCountry.dialCode();

        if (user.getAddress() != null) {
            street = user.getAddress().getStreet();
            city = user.getAddress().getCity();
            state = user.getAddress().getState();
            postalCode = user.getAddress().getPostalCode();
        }

        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhoneNumber(),
                street,
                city,
                state,
                postalCode,
                countryCode,
                countryName,
                countryDialCode,
                user.getBillingLegalName(),
                user.getTaxRegistrationNumber(),
                user.isEnabled(),
                user.isEmailVerified(),
                user.getCreatedAt()
        );
    }

    /**
     * Resolves a complete, non-null country tuple for profile responses.
     * Address data is authoritative. Legacy registrations without an address
     * are inferred from the international phone prefix, with US as the
     * platform default when neither source identifies a country.
     */
    private ResolvedCountry resolveProfileCountry(User user) {
        if (user.getAddress() != null && user.getAddress().getCountry() != null) {
            Country country = user.getAddress().getCountry();
            return new ResolvedCountry(country.getIsoCode(), country.getName(), country.getDialCode());
        }

        String phoneNumber = normalizeOptionalText(user.getPhoneNumber());
        if (phoneNumber != null && phoneNumber.startsWith("+")) {
            var inferred = countryRepository.findByEnabledTrueOrderByNameAsc().stream()
                    .filter(country -> phoneNumber.startsWith(country.getDialCode()))
                    .sorted(Comparator
                            .comparingInt((Country country) -> country.getDialCode().length()).reversed()
                            .thenComparingInt(country -> "US".equals(country.getIsoCode()) ? 0 : 1))
                    .findFirst();
            if (inferred.isPresent()) {
                Country country = inferred.get();
                return new ResolvedCountry(country.getIsoCode(), country.getName(), country.getDialCode());
            }
        }

        return countryRepository.findByIsoCodeAndEnabledTrue("US")
                .map(country -> new ResolvedCountry(country.getIsoCode(), country.getName(), country.getDialCode()))
                .orElseGet(() -> new ResolvedCountry("US", "United States", "+1"));
    }

    private record ResolvedCountry(String code, String name, String dialCode) {
        private ResolvedCountry {
            Objects.requireNonNull(code, "code");
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(dialCode, "dialCode");
        }
    }

    /**
     * Executes to admin detail for `UserManagementService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.service`.
     * @param user input consumed by toAdminDetail.
     * @return result produced by toAdminDetail.
     */
    private AdminUserDetailResponse toAdminDetail(User user) {
        String street = null;
        String city = null;
        String state = null;
        String postalCode = null;
        String countryCode = null;
        String countryName = null;
        String countryDialCode = null;

        if (user.getAddress() != null) {
            street = user.getAddress().getStreet();
            city = user.getAddress().getCity();
            state = user.getAddress().getState();
            postalCode = user.getAddress().getPostalCode();
            if (user.getAddress().getCountry() != null) {
                countryCode = user.getAddress().getCountry().getIsoCode();
                countryName = user.getAddress().getCountry().getName();
                countryDialCode = user.getAddress().getCountry().getDialCode();
            }
        }

        return new AdminUserDetailResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhoneNumber(),
                street,
                city,
                state,
                postalCode,
                countryCode,
                countryName,
                countryDialCode,
                user.isEnabled(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getRoles().stream().map(role -> role.getName()).sorted().toList()
        );
    }

    private AdminUserDirectoryEntryResponse toAdminDirectoryEntry(User user) {
        return new AdminUserDirectoryEntryResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRoles().stream().map(role -> role.getName()).sorted().toList()
        );
    }
}
