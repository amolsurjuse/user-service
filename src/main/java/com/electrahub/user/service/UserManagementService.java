package com.electrahub.user.service;

import com.electrahub.user.api.dto.AddressDto;
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
import org.springframework.data.domain.PageRequest;
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
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
        return toProfile(user);
    }

    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UpdateUserProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        user.setFirstName(normalizeText(request.firstName()));
        user.setLastName(normalizeText(request.lastName()));

        AddressDto addressDto = request.address();
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
        } else {
            Address address = user.getAddress();
            address.setStreet(normalizeText(addressDto.street()));
            address.setCity(normalizeText(addressDto.city()));
            address.setState(normalizeText(addressDto.state()));
            address.setPostalCode(normalizeText(addressDto.postalCode()));
            address.setCountry(country);
        }

        return toProfile(user);
    }

    @Transactional(readOnly = true)
    public UserSearchResponse search(String query, int limit, int offset) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        int safeOffset = Math.max(0, offset);

        int page = safeOffset / safeLimit;
        var pageable = PageRequest.of(page, safeLimit);

        var pageResult = userRepository.search(normalizeQuery(query), pageable);
        var items = pageResult.getContent().stream()
                .map(this::toSummary)
                .toList();

        long total = userRepository.countSearch(normalizeQuery(query));
        return new UserSearchResponse(items, total, safeLimit, safeOffset);
    }

    @Transactional(readOnly = true)
    public UserCountResponse count(String query) {
        long count = userRepository.countSearch(normalizeQuery(query));
        return new UserCountResponse(normalizeQuery(query), count);
    }

    @Transactional(readOnly = true)
    public List<CountryResponse> countries() {
        return countryRepository.findByEnabledTrueOrderByNameAsc().stream()
                .map(c -> new CountryResponse(c.getIsoCode(), c.getName(), c.getDialCode()))
                .toList();
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
}
