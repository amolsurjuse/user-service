package com.electrahub.user.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;

@Component
public class PaymentAccountStateClient {

    private static final Logger log = LoggerFactory.getLogger(PaymentAccountStateClient.class);

    private final RestClient restClient;

    public PaymentAccountStateClient(@Value("${app.payment-service.base-url}") String paymentBaseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(paymentBaseUrl)
                .build();
    }

    public BigDecimal walletBalance(String accountId) {
        try {
            PaymentStateResponse response = restClient.get()
                    .uri("/api/v1/payment/state")
                    .header("X-Account-Id", accountId)
                    .retrieve()
                    .body(PaymentStateResponse.class);
            if (response == null || response.wallet() == null || response.wallet().balance() == null) {
                return BigDecimal.ZERO;
            }
            return response.wallet().balance();
        } catch (RestClientResponseException ex) {
            log.warn("Unable to fetch wallet state for accountId={} status={} body={}",
                    accountId,
                    ex.getStatusCode().value(),
                    ex.getResponseBodyAsString());
            throw ex;
        }
    }

    public record PaymentStateResponse(
            WalletStateResponse wallet
    ) {
    }

    public record WalletStateResponse(
            BigDecimal balance
    ) {
    }
}

