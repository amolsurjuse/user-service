package com.electrahub.user.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TermsActivationScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TermsActivationScheduler.class);

    private final TermsService termsService;

    public TermsActivationScheduler(TermsService termsService) {
        this.termsService = termsService;
    }

    @Scheduled(fixedDelayString = "${app.terms.activation-check-delay-ms:300000}")
    public void activateDueVersions() {
        // TODO: MDC/trace context not propagated across this async boundary — see ElectraHub Logging Standard
        LOGGER.debug("Running scheduled terms activation check");
        termsService.activateDueVersions();
        LOGGER.debug("Scheduled terms activation check completed");
    }
}
