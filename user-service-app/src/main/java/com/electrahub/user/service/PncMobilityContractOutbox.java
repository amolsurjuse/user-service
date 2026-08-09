package com.electrahub.user.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PncMobilityContractOutbox {
    public static final String EVENT_TYPE = "DriverMobilityContractRequired.v1";
    public static final String TENANT_HEADER = "x-electrahub-tenant-id";
    private static final Logger log = LoggerFactory.getLogger(PncMobilityContractOutbox.class);

    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final RabbitTemplate rabbit;
    private final boolean publisherEnabled;
    private final boolean backfillEnabled;
    private final String exchange;
    private final String routingKey;
    private final String providerId;
    private final int batchSize;

    public PncMobilityContractOutbox(JdbcTemplate jdbc, ObjectMapper json, RabbitTemplate rabbit,
            @Value("${app.pnc.publisher-enabled:false}") boolean publisherEnabled,
            @Value("${app.pnc.backfill-enabled:false}") boolean backfillEnabled,
            @Value("${app.pnc.exchange:pnc.events}") String exchange,
            @Value("${app.pnc.driver-contract-routing-key:driver.mobility-contract.required}") String routingKey,
            @Value("${app.pnc.provider-id:EHB}") String providerId,
            @Value("${app.pnc.outbox.batch-size:25}") int batchSize) {
        this.jdbc = jdbc; this.json = json; this.rabbit = rabbit;
        this.publisherEnabled = publisherEnabled; this.backfillEnabled = backfillEnabled;
        this.exchange = exchange; this.routingKey = routingKey; this.providerId = providerId.toUpperCase();
        this.batchSize = Math.max(1, Math.min(batchSize, 100));
    }

    public void enqueue(String tenantId, UUID userId, String countryCode) {
        UUID eventId = stableId(tenantId, userId);
        var event = new DriverMobilityContractRequired(eventId, EVENT_TYPE, tenantId, userId,
                countryCode.toUpperCase(), providerId, OffsetDateTime.now(ZoneOffset.UTC), eventId);
        try {
            jdbc.update("""
                    INSERT INTO user_mgmt.pnc_contract_outbox_event
                        (id,tenant_id,aggregate_id,event_type,payload,status,created_at)
                    VALUES (?,?,?,?,?::jsonb,'PENDING',now())
                    ON CONFLICT (tenant_id,aggregate_id,event_type) DO NOTHING
                    """, eventId, tenantId, userId, EVENT_TYPE, json.writeValueAsString(event));
        } catch (Exception failure) {
            throw new IllegalStateException("Unable to enqueue Plug & Charge mobility contract event", failure);
        }
    }

    @Scheduled(fixedDelayString = "${app.pnc.outbox.publish-delay-ms:5000}")
    @Transactional
    public void publishPending() {
        if (!publisherEnabled) return;
        List<OutboxRow> rows = jdbc.query("""
                SELECT id,tenant_id,payload::text payload FROM user_mgmt.pnc_contract_outbox_event
                 WHERE status IN ('PENDING','FAILED') AND attempts < 20
                 ORDER BY created_at LIMIT ? FOR UPDATE SKIP LOCKED
                """, (rs, n) -> new OutboxRow(rs.getObject("id", UUID.class), rs.getString("tenant_id"),
                        rs.getString("payload")), batchSize);
        for (var row : rows) {
            try {
                var event = json.readValue(row.payload(), DriverMobilityContractRequired.class);
                rabbit.invoke(operations -> {
                    operations.convertAndSend(exchange, routingKey, event, message -> {
                        MessageProperties properties = message.getMessageProperties();
                        properties.setMessageId(row.id().toString());
                        properties.setHeader(TENANT_HEADER, row.tenantId());
                        properties.setHeader("event_type", EVENT_TYPE);
                        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
                        return message;
                    });
                    operations.waitForConfirmsOrDie(5_000L);
                    return null;
                });
                jdbc.update("UPDATE user_mgmt.pnc_contract_outbox_event SET status='PUBLISHED',published_at=now(),attempts=attempts+1,last_error=NULL WHERE id=?", row.id());
            } catch (Exception failure) {
                jdbc.update("UPDATE user_mgmt.pnc_contract_outbox_event SET status='FAILED',attempts=attempts+1,last_error=? WHERE id=?",
                        truncate(failure.getMessage()), row.id());
                log.warn("PnC mobility-contract event delivery failed eventId={}: {}", row.id(), failure.getMessage());
            }
        }
    }

    @Scheduled(initialDelayString = "${app.pnc.backfill-initial-delay-ms:30000}",
            fixedDelayString = "${app.pnc.backfill-delay-ms:3600000}")
    public void backfillDrivers() {
        if (!backfillEnabled) return;
        var drivers = jdbc.query("""
                SELECT DISTINCT u.tenant_id,u.id,COALESCE(c.iso_code,'US') country_code
                  FROM user_mgmt.users u
                  JOIN user_mgmt.user_roles ur ON ur.user_id=u.id
                  JOIN user_mgmt.roles r ON r.id=ur.role_id AND r.name='CUSTOMER'
                  LEFT JOIN user_mgmt.address a ON a.id=u.address_id
                  LEFT JOIN user_mgmt.country c ON c.id=a.country_id
                 WHERE u.enabled=true
                """, (rs, n) -> new Driver(rs.getString(1), rs.getObject(2, UUID.class), rs.getString(3)));
        drivers.forEach(driver -> enqueue(driver.tenantId(), driver.userId(), driver.countryCode()));
        log.info("PnC mobility-contract backfill converged {} enabled drivers", drivers.size());
    }

    static UUID stableId(String tenantId, UUID userId) {
        return UUID.nameUUIDFromBytes(("pnc-driver-contract|" + tenantId + "|" + userId)
                .getBytes(StandardCharsets.UTF_8));
    }

    private static String truncate(String value) {
        if (value == null) return "unknown";
        return value.length() <= 500 ? value : value.substring(0, 500);
    }

    public record DriverMobilityContractRequired(UUID eventId, String eventType, String tenantId, UUID userId,
            String countryCode, String providerId, OffsetDateTime occurredAt, UUID correlationId) {}
    private record Driver(String tenantId, UUID userId, String countryCode) {}
    private record OutboxRow(UUID id, String tenantId, String payload) {}
}
