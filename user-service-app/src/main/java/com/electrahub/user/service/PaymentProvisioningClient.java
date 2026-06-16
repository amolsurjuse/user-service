package com.electrahub.user.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;

@Component
public class PaymentProvisioningClient {

    private static final Logger log = LoggerFactory.getLogger(PaymentProvisioningClient.class);

    private final RestClient restClient;

    /**
     * Executes value for `PaymentProvisioningClient`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.service`.
     * @param paymentBaseUrl input consumed by Value.
     * @return result produced by Value.
     */
    public PaymentProvisioningClient(@Value("${app.payment-service.base-url}") String paymentBaseUrl) {
        log.info("Configuring payment provisioning client with baseUrl={}", paymentBaseUrl);
        this.restClient = RestClient.builder()
                .baseUrl(paymentBaseUrl)
                .build();
    }

    /**
     * Creates create wallet for `PaymentProvisioningClient`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.service`.
     * @param accountId input consumed by createWallet.
     * @param countryCode input consumed by createWallet.
     */
    public void createWallet(String accountId, String countryCode) {
        WalletCreateRequest payload = new WalletCreateRequest(accountId, BigDecimal.ZERO, null, null, countryCode);
        log.info("Requesting wallet provisioning for accountId={} countryCode={}", accountId, countryCode);

        try {
            restClient.post()
                    .uri("/api/v1/payment/internal/accounts")
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Wallet provisioning succeeded for accountId={}", accountId);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 409) {
                log.info("Payment account already exists for accountId={}", accountId);
                return;
            }
            log.error("Wallet provisioning failed for accountId={} status={} body={}",
                    accountId, ex.getStatusCode().value(), ex.getResponseBodyAsString(), ex);
            throw ex;
        }
    }

    public record WalletCreateRequest(
            String accountId,
            BigDecimal initialBalance,
            BigDecimal budget,
            String currency,
            String countryCode
    ) {
    }
}
