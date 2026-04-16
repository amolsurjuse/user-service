package com.electrahub.user.service;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.electrahub.user.api.dto.AddressDto;
import com.electrahub.user.api.dto.AdminResetPasswordRequest;
import com.electrahub.user.api.dto.AdminUpdateUserRequest;
import com.electrahub.user.api.dto.AdminUserDetailResponse;
import com.electrahub.user.api.dto.AdminUserSearchResponse;
import com.electrahub.user.api.dto.AdminUserSummaryResponse;
import com.electrahub.user.api.dto.AccountDeletionDecision;
import com.electrahub.user.api.dto.AccountDeletionResponse;
import com.electrahub.user.api.dto.AuthenticateUserRequest;
import com.electrahub.user.api.dto.CountryResponse;
import com.electrahub.user.api.dto.RegisterUserRequest;
import com.electrahub.user.api.dto.UpdateUserProfileRequest;
import com.electrahub.user.api.dto.UserCountResponse;
import com.electrahub.user.api.dto.UserProfileResponse;
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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class UserManagementService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserManagementService.class);


    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AddressRepository addressRepository;
    private final CountryRepository countryRepository;
    private final PaymentProvisioningClient paymentProvisioningClient;
    private final PaymentAccountStateClient paymentAccountStateClient;
    private final SessionActivityClient sessionActivityClient;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserManagementService(UserRepository userRepository,
                                 RoleRepository roleRepository,
                                 AddressRepository addressRepository,
                                 CountryRepository countryRepository,
                                 PaymentProvisioningClient paymentProvisioningClient,
                                 PaymentAccountStateClient paymentAccountStateClient,
                                 SessionActivityClient sessionActivityClient) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.addressRepository = addressRepository;
        this.countryRepository = countryRepository;
        this.paymentProvisioningClient = paymentProvisioningClient;
        this.paymentAccountStateClient = paymentAccountStateClient;
        this.sessionActivityClient = sessionActivityClient;
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
        LOGGER.info("CODEx_ENTRY_LOG: Entering UserManagementService#register");
        LOGGER.debug("CODEx_ENTRY_LOG: Entering UserManagementService#register with debug context");
        String normalizedEmail = normalizeEmail(request.email());
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ConflictException("Email already registered");
        }

        OffsetDateTime now = OffsetDateTime.now();
        User user = new User(UUID.randomUUID(), normalizedEmail, passwordEncoder.encode(request.password()), true, now);
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setPhoneNumber(request.phoneNumber());

        Address address = buildAddress(request.address());
        if (address != null) {
            addressRepository.save(address);
            user.setAddress(address);
        }

        var userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new IllegalStateException("Role USER not seeded"));
        user.addRole(userRole);

        userRepository.save(user);

        // Keep user creation and wallet provisioning in one business flow.
        paymentProvisioningClient.createWallet(user.getId().toString(), resolveCountryCode(user));

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
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (!user.isEnabled()) {
            throw new UnauthorizedException("User is disabled");
        }
        if (user.isPendingDeletion()) {
            throw new UnauthorizedException("User account is pending deletion");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }

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
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        user.setFirstName(normalizeText(request.firstName()));
        user.setLastName(normalizeText(request.lastName()));
        applyAddress(user, request.address());

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
        if (!actor.hasRole("SYSTEM_ADMIN")) {
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
        if (!actor.hasRole("SYSTEM_ADMIN")) {
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

        var pageResult = userRepository.searchSystemAdmins(normalizedQuery, PageRequest.of(page, safeLimit));
        var items = pageResult.getContent().stream()
                .map(this::toAdminSummary)
                .toList();
        long total = userRepository.countSearchSystemAdmins(normalizedQuery);
        int totalPages = Math.max(pageResult.getTotalPages(), total > 0 ? 1 : 0);

        return new AdminUserSearchResponse(items, total, safeLimit, safeOffset, page, totalPages, pageResult.hasNext(), pageResult.hasPrevious());
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
        if (actor.userId().equals(userId) && !request.enabled()) {
            throw new AccessDeniedException("You cannot disable your own account.");
        }

        User user = loadUser(userId);
        user.setFirstName(normalizeText(request.firstName()));
        user.setLastName(normalizeText(request.lastName()));
        user.setPhoneNumber(normalizeText(request.phoneNumber()));
        user.setEnabled(request.enabled());
        applyAddress(user, request.address());

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
        if (actor.userId().equals(userId)) {
            throw new AccessDeniedException("You cannot delete your own account from the admin console.");
        }

        User user = loadUser(userId);
        hardDeleteUser(user);
    }

    @Transactional
    public AccountDeletionResponse requestAccountDeletion(UUID userId, boolean confirmDirectDeletion) {
        requireSelfOrSystemAdmin(userId);
        User user = loadUser(userId);

        if (user.isPendingDeletion()) {
            return new AccountDeletionResponse(
                    AccountDeletionDecision.ALREADY_PENDING_DELETION,
                    "Your account is already marked for pending deletion review.",
                    BigDecimal.ZERO,
                    false,
                    true,
                    false,
                    false,
                    user.getDeletionRequestedAt()
            );
        }

        String accountId = user.getId().toString();
        boolean hasActiveChargingSession = sessionActivityClient.hasActiveChargingSession(accountId);
        BigDecimal walletBalance = paymentAccountStateClient.walletBalance(accountId);

        if (hasActiveChargingSession) {
            return new AccountDeletionResponse(
                    AccountDeletionDecision.ACTIVE_SESSION_IN_PROGRESS,
                    "Please finish your active charging session before deleting your account.",
                    walletBalance,
                    true,
                    false,
                    false,
                    false,
                    null
            );
        }

        if (walletBalance.compareTo(BigDecimal.ZERO) > 0) {
            OffsetDateTime requestedAt = OffsetDateTime.now();
            user.setPendingDeletion(true);
            user.setDeletionRequestedAt(requestedAt);
            user.setEnabled(false);
            userRepository.save(user);

            return new AccountDeletionResponse(
                    AccountDeletionDecision.ACCOUNT_MARKED_PENDING_DELETION,
                    "Account deletion request submitted. Your account is now pending deletion review in admin portal.",
                    walletBalance,
                    false,
                    true,
                    false,
                    false,
                    requestedAt
            );
        }

        if (!confirmDirectDeletion) {
            return new AccountDeletionResponse(
                    AccountDeletionDecision.CONFIRM_DIRECT_DELETION,
                    "Wallet balance is zero and no active charging was found. Confirm to delete your account permanently.",
                    walletBalance,
                    false,
                    false,
                    false,
                    true,
                    null
            );
        }

        hardDeleteUser(user);
        return new AccountDeletionResponse(
                AccountDeletionDecision.ACCOUNT_DELETED,
                "Your account has been deleted successfully.",
                walletBalance,
                false,
                false,
                true,
                false,
                null
        );
    }

    private void hardDeleteUser(User user) {
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
                user.isPendingDeletion(),
                user.getRoles().stream().map(role -> role.getName()).toList()
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
                user.isPendingDeletion(),
                user.getDeletionRequestedAt(),
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
                user.isPendingDeletion(),
                user.getDeletionRequestedAt(),
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
                user.isEnabled(),
                user.isPendingDeletion(),
                user.getDeletionRequestedAt(),
                user.getCreatedAt()
        );
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
                user.isPendingDeletion(),
                user.getDeletionRequestedAt(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getRoles().stream().map(role -> role.getName()).sorted().toList()
        );
    }
}
