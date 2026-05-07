package com.electrahub.user.service;

import com.electrahub.user.domain.TermsVersion;
import com.electrahub.user.repository.TermsAcceptanceRepository;
import com.electrahub.user.repository.TermsVersionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TermsServiceTest {

    private final TermsVersionRepository termsVersionRepository = mock(TermsVersionRepository.class);
    private final TermsAcceptanceRepository termsAcceptanceRepository = mock(TermsAcceptanceRepository.class);
    private final TermsService termsService = new TermsService(termsVersionRepository, termsAcceptanceRepository);

    @Test
    void currentTermsIncludesContentText() {
        TermsVersion active = activeTermsVersion("ELECTRA HUB TERMS & CONDITIONS\n\nEffective Date: May 7, 2026");
        when(termsVersionRepository.findByActiveTrue()).thenReturn(Optional.of(active));

        var response = termsService.currentTerms(TermsAudience.ADMIN_PORTAL);

        assertThat(response.contentText()).isEqualTo(active.getContentText());
        assertThat(response.contentUrl()).isEqualTo("https://cdn.electrahub.com/legal/terms/v1.html");
        assertThat(response.contentSha256()).isEqualTo("a1b2c3d4");
    }

    @Test
    void currentTermsReturnsDriverTextWhenStoredVersionIsAdminSpecific() {
        TermsVersion active = activeTermsVersion("Continued use of the Admin Portal confirms acceptance.");
        when(termsVersionRepository.findByActiveTrue()).thenReturn(Optional.of(active));

        var response = termsService.currentTerms(TermsAudience.DRIVER_PORTAL);

        assertThat(response.contentText()).contains("Driver App");
        assertThat(response.contentText()).doesNotContain("Admin Portal");
    }

    @Test
    void gateStatusIncludesContentTextWhenAcceptanceIsRequired() {
        UUID userId = UUID.randomUUID();
        TermsVersion active = activeTermsVersion("Plain-text terms for portal display");
        when(termsVersionRepository.findByActiveTrue()).thenReturn(Optional.of(active));
        when(termsAcceptanceRepository.findTopByUserIdOrderByTermsVersionVersionNumberDescAcceptedAtDesc(userId))
                .thenReturn(Optional.empty());
        when(termsAcceptanceRepository.findByUserIdAndTermsVersionId(userId, active.getId()))
                .thenReturn(Optional.empty());

        var response = termsService.gateStatus(userId, TermsAudience.DRIVER_PORTAL);

        assertThat(response.termsAccepted()).isFalse();
        assertThat(response.contentText()).isEqualTo("Plain-text terms for portal display");
        assertThat(response.currentVersionLabel()).isEqualTo("May 2026");
    }

    private TermsVersion activeTermsVersion(String contentText) {
        TermsVersion version = new TermsVersion(
                UUID.randomUUID(),
                1,
                "May 2026",
                "https://cdn.electrahub.com/legal/terms/v1.html",
                "a1b2c3d4",
                contentText,
                true,
                OffsetDateTime.parse("2026-05-07T14:16:27.805852Z"),
                UUID.randomUUID(),
                OffsetDateTime.parse("2026-05-07T14:16:27.805852Z")
        );
        version.activate();
        return version;
    }
}
