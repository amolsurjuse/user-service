package com.electrahub.user.config;

import com.electrahub.user.domain.Role;
import com.electrahub.user.domain.User;
import com.electrahub.user.repository.RoleRepository;
import com.electrahub.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "app.dev-admin", name = "enabled", havingValue = "true")
public class DevelopmentAdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevelopmentAdminSeeder.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${app.dev-admin.email}")
    private String email;

    @Value("${app.dev-admin.password}")
    private String password;

    @Value("${app.dev-admin.first-name}")
    private String firstName;

    @Value("${app.dev-admin.last-name}")
    private String lastName;

    @Value("${app.dev-admin.phone-number}")
    private String phoneNumber;

    @Value("${app.dev-user.password:User@12345}")
    private String devUserPassword;

    /**
     * Executes development admin seeder for `DevelopmentAdminSeeder`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.config`.
     * @param userRepository input consumed by DevelopmentAdminSeeder.
     * @param roleRepository input consumed by DevelopmentAdminSeeder.
     */
    public DevelopmentAdminSeeder(UserRepository userRepository, RoleRepository roleRepository) {
        log.info("CODEx_ENTRY_LOG: Entering DevelopmentAdminSeeder#DevelopmentAdminSeeder");
        log.debug("CODEx_ENTRY_LOG: Entering DevelopmentAdminSeeder#DevelopmentAdminSeeder with debug context");
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    /**
     * Executes run for `DevelopmentAdminSeeder`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.config`.
     * @param args input consumed by run.
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        var systemAdminRole = loadRole("SYSTEM_ADMIN");
        var userRole = loadRole("USER");
        Map<String, Role> domainRoles = Map.of(
                "NETWORK", loadRole("NETWORK"),
                "ENTERPRISE", loadRole("ENTERPRISE"),
                "LOCATION", loadRole("LOCATION"),
                "SITE", loadRole("SITE"),
                "SUPPORT", loadRole("SUPPORT")
        );

        List<DevUserSeed> adminSeeds = List.of(
                new DevUserSeed(email, password, firstName, lastName, phoneNumber, true, null),
                new DevUserSeed("network.operator.dev@electrahub.com", password, "Network", "Operator", "+15550000002", false, "NETWORK"),
                new DevUserSeed("enterprise.operator.dev@electrahub.com", password, "Enterprise", "Operator", "+15550000003", false, "ENTERPRISE"),
                new DevUserSeed("location.operator.dev@electrahub.com", password, "Location", "Operator", "+15550000004", false, "LOCATION"),
                new DevUserSeed("site.operator.dev@electrahub.com", password, "Site", "Operator", "+15550000005", false, "SITE"),
                new DevUserSeed("support.operator.dev@electrahub.com", password, "Support", "Operator", "+15550000006", false, "SUPPORT")
        );

        for (DevUserSeed seed : adminSeeds) {
            ensureUser(seed, systemAdminRole, userRole, domainRoles);
        }

        ensureUser(
                new DevUserSeed("driver.user.dev@electrahub.com", devUserPassword, "Driver", "User", "+15550000007", false, null),
                systemAdminRole,
                userRole,
                domainRoles
        );
    }

    /**
     * Executes ensure user for `DevelopmentAdminSeeder`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.config`.
     * @param seed input consumed by ensureUser.
     * @param systemAdminRole input consumed by ensureUser.
     * @param userRole input consumed by ensureUser.
     * @param Map<String input consumed by ensureUser.
     * @param domainRoles input consumed by ensureUser.
     */
    private void ensureUser(DevUserSeed seed, Role systemAdminRole, Role userRole, Map<String, Role> domainRoles) {
        String normalizedEmail = seed.email().trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseGet(() -> createUser(normalizedEmail, seed.password()));

        user.setEnabled(true);
        user.setFirstName(seed.firstName());
        user.setLastName(seed.lastName());
        user.setPhoneNumber(seed.phoneNumber());
        user.setPasswordHash(passwordEncoder.encode(seed.password()));
        user.addRole(userRole);
        if (seed.systemAdmin()) {
            user.addRole(systemAdminRole);
        } else {
            user.getRoles().removeIf(role -> "SYSTEM_ADMIN".equals(role.getName()));
        }

        user.getRoles().removeIf(role -> domainRoles.containsKey(role.getName()));
        if (seed.domainRoleName() != null) {
            Role domainRole = domainRoles.get(seed.domainRoleName());
            if (domainRole == null) {
                throw new IllegalStateException("Domain role not configured: " + seed.domainRoleName());
            }
            user.addRole(domainRole);
        }

        userRepository.save(user);
        log.info(
                "Ensured development user exists: {} (systemAdmin={}, domainRole={})",
                normalizedEmail,
                seed.systemAdmin(),
                seed.domainRoleName()
        );
    }

    /**
     * Retrieves load role for `DevelopmentAdminSeeder`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.config`.
     * @param roleName input consumed by loadRole.
     * @return result produced by loadRole.
     */
    private Role loadRole(String roleName) {
        return roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalStateException("Role " + roleName + " not seeded"));
    }

    /**
     * Creates create user for `DevelopmentAdminSeeder`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.config`.
     * @param normalizedEmail input consumed by createUser.
     * @param rawPassword input consumed by createUser.
     * @return result produced by createUser.
     */
    private User createUser(String normalizedEmail, String rawPassword) {
        OffsetDateTime now = OffsetDateTime.now();
        return new User(
                UUID.randomUUID(),
                normalizedEmail,
                passwordEncoder.encode(rawPassword),
                true,
                now
        );
    }

    private record DevUserSeed(
            String email,
            String password,
            String firstName,
            String lastName,
            String phoneNumber,
            boolean systemAdmin,
            String domainRoleName
    ) {
    }
}
