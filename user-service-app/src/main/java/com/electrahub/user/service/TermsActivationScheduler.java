package com.electrahub.user.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TermsActivationScheduler {

    private final TermsService termsService;

    public TermsActivationScheduler(TermsService termsService) {
        this.termsService = termsService;
    }

    @Scheduled(fixedDelayString = "${app.terms.activation-check-delay-ms:300000}")
    public void activateDueVersions() {
        termsService.activateDueVersions();
    }
}
