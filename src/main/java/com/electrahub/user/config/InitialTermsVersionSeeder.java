package com.electrahub.user.config;

import com.electrahub.user.domain.TermsVersion;
import com.electrahub.user.repository.TermsVersionRepository;
import com.electrahub.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
@Order(20)
@ConditionalOnProperty(prefix = "app.terms.initial-version", name = "enabled", havingValue = "true", matchIfMissing = true)
public class InitialTermsVersionSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InitialTermsVersionSeeder.class);

    private final TermsVersionRepository termsVersionRepository;
    private final UserRepository userRepository;

    @Value("${app.terms.initial-version.admin-email:${app.dev-admin.email:sysadmin.dev@electrahub.com}}")
    private String adminEmail;

    @Value("${app.terms.initial-version.version-label:May 2026}")
    private String versionLabel;

    @Value("${app.terms.initial-version.content-url:https://cdn.electrahub.com/legal/terms/v1.html}")
    private String contentUrl;

    @Value("${app.terms.initial-version.content-sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855}")
    private String contentSha256;

    @Value("${app.terms.initial-version.requires-re-acceptance:true}")
    private boolean requiresReAcceptance;

    public InitialTermsVersionSeeder(TermsVersionRepository termsVersionRepository, UserRepository userRepository) {
        this.termsVersionRepository = termsVersionRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (termsVersionRepository.findByActiveTrue().isPresent()) {
            return;
        }

        String normalizedAdminEmail = adminEmail == null ? "" : adminEmail.trim().toLowerCase();
        userRepository.findByEmail(normalizedAdminEmail).ifPresentOrElse(
                admin -> {
                    OffsetDateTime now = OffsetDateTime.now();
                    TermsVersion version = new TermsVersion(
                            UUID.randomUUID(),
                            1,
                            versionLabel.trim(),
                            contentUrl.trim(),
                            contentSha256.trim().toLowerCase(),
                            requiresReAcceptance,
                            now,
                            admin.getId(),
                            now
                    );
                    version.activate();
                    termsVersionRepository.save(version);
                    log.info("Seeded initial active Terms version {} by {}", versionLabel, normalizedAdminEmail);
                },
                () -> log.warn("Skipping initial Terms seed because admin user {} does not exist", normalizedAdminEmail)
        );
    }
}
