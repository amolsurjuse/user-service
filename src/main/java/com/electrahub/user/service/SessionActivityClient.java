package com.electrahub.user.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

@Component
public class SessionActivityClient {

    private static final Logger log = LoggerFactory.getLogger(SessionActivityClient.class);
    private static final ParameterizedTypeReference<List<Map<String, Object>>> ACTIVE_SESSIONS_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;

    public SessionActivityClient(@Value("${app.session-service.base-url}") String sessionBaseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(sessionBaseUrl)
                .build();
    }

    public boolean hasActiveChargingSession(String accountId) {
        try {
            List<Map<String, Object>> response = restClient.get()
                    .uri("/api/v1/sessions/active")
                    .header("X-Account-Id", accountId)
                    .retrieve()
                    .body(ACTIVE_SESSIONS_TYPE);
            return response != null && !response.isEmpty();
        } catch (RestClientResponseException ex) {
            log.warn("Unable to fetch active charging sessions for accountId={} status={} body={}",
                    accountId,
                    ex.getStatusCode().value(),
                    ex.getResponseBodyAsString());
            throw ex;
        }
    }
}

