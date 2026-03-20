package com.electrahub.user.service;

import com.electrahub.user.api.dto.AddressDto;
import com.electrahub.user.api.dto.AdminResetPasswordRequest;
import com.electrahub.user.api.dto.AdminUpdateUserRequest;
import com.electrahub.user.api.dto.AdminUserDetailResponse;
import com.electrahub.user.api.dto.AdminUserSearchResponse;
import com.electrahub.user.api.dto.AdminUserSummaryResponse;
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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class UserManagementService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AddressRepository addressRepository;
    private final CountryRepository countryRepository;
    private final PaymentProvisioningClient paymentProvisioningClient;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserManagementService(UserRepository userRepository,
                                 RoleRepository roleRepository,
                                 AddressRepository addressRepository,
                                 CountryRepository countryRepository,
                                 PaymentProvisioningClient paymentProvisioningClient) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.addressRepository = addressRepository;
        this.countryRepository = countryRepository;
        this.paymentProvisioningClient = paymentProvisioningClient;
    }

    @Transactional
    public UserPrincipalResponse register(RegisterUserRequest request) {
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

    @Transactional(readOnly = true)
    public UserPrincipalResponse authenticate(AuthenticateUserRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (!user.isEnabled()) {
            throw new UnauthorizedException("User is disabled");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        return toPrincipal(user);
    }

    @Transactional(readOnly = true)
    public UserPrincipalResponse getPrincipal(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
        return toPrincipal(user);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID userId) {
        requireSelfOrSystemAdmin(userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
        return toProfile(user);
    }

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

    @Transactional(readOnly = true)
    public List<CountryResponse> countries() {
        return countryRepository.findByEnabledTrueOrderByNameAsc().stream()
                .map(c -> new CountryResponse(c.getIsoCode(), c.getName(), c.getDialCode()))
                .toList();
    }

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

    @Transactional(readOnly = true)
    public AdminUserDetailResponse getAdminUser(UUID userId) {
        return toAdminDetail(loadUser(userId));
    }

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

    @Transactional
    public void resetPassword(UUID userId, AdminResetPasswordRequest request) {
        User user = loadUser(userId);
        user.setPasswordHash(passwordEncoder.encode(request.newPassword().trim()));
    }

    @Transactional
    public void deleteUser(UUID userId) {
        AuthenticatedUser actor = currentUser();
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
    }

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

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeQuery(String query) {
        return query == null ? "" : query.trim();
    }

    private Country resolveCountry(String countryIsoCode) {
        String code = normalizeText(countryIsoCode).toUpperCase();
        if (code.isBlank()) {
            return null;
        }
        return countryRepository.findByIsoCodeAndEnabledTrue(code)
                .orElseThrow(() -> new IllegalArgumentException("Country not available"));
    }

    private String resolveCountryCode(User user) {
        if (user.getAddress() == null || user.getAddress().getCountry() == null) {
            return "US";
        }
        return user.getAddress().getCountry().getIsoCode();
    }

    private AuthenticatedUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new UnauthorizedException("Authentication required");
        }
        return user;
    }

    private void requireSelfOrSystemAdmin(UUID userId) {
        AuthenticatedUser actor = currentUser();
        if (!actor.hasRole("SYSTEM_ADMIN") && !actor.userId().equals(userId)) {
            throw new AccessDeniedException("You are not allowed to access this user.");
        }
    }

    private User loadUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
    }

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

    private boolean contains(String source, String query) {
        return source != null && source.toLowerCase().contains(query);
    }

    private UserPrincipalResponse toPrincipal(User user) {
        return new UserPrincipalResponse(
                user.getId(),
                user.getEmail(),
                user.isEnabled(),
                user.getRoles().stream().map(role -> role.getName()).toList()
        );
    }

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
                user.getCreatedAt()
        );
    }

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
}
