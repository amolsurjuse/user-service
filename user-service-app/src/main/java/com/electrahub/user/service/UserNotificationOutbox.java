package com.electrahub.user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class UserNotificationOutbox {
    private static final Logger log = LoggerFactory.getLogger(UserNotificationOutbox.class);

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String routingKey;
    private final String tenantId;
    private final int batchSize;

    public UserNotificationOutbox(
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            RabbitTemplate rabbitTemplate,
            @Value("${app.notification.exchange:notifications.events}") String exchange,
            @Value("${app.notification.domain-routing-key:notifications.domain}") String routingKey,
            @Value("${app.notification.tenant-id:electrahub}") String tenantId,
            @Value("${app.notification.outbox.batch-size:25}") int batchSize
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.tenantId = tenantId;
        this.batchSize = Math.max(1, Math.min(batchSize, 100));
    }

    public void enqueue(String eventType, UUID userId, Map<String, Object> payload) {
        UUID eventId = UUID.randomUUID();
        String recipient = userId.toString();
        DomainNotificationEvent event = new DomainNotificationEvent(
                eventId.toString(),
                eventType,
                tenantId,
                recipient,
                recipient,
                OffsetDateTime.now(ZoneOffset.UTC).toString(),
                payload == null ? Map.of() : Map.copyOf(payload)
        );

        try {
            jdbcTemplate.update(
                    """
                    INSERT INTO user_mgmt.notification_outbox_event
                        (id, aggregate_type, aggregate_id, event_type, payload, status, created_at)
                    VALUES (?, 'USER', ?, ?, ?::jsonb, 'PENDING', now())
                    """,
                    eventId,
                    recipient,
                    eventType,
                    objectMapper.writeValueAsString(event)
            );
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize user notification event " + eventType, ex);
        }
    }

    @Scheduled(fixedDelayString = "${app.notification.outbox.publish-delay-ms:5000}")
    @Transactional
    public void publishPending() {
        List<OutboxRow> rows = jdbcTemplate.query(
                """
                SELECT id, event_type, payload::text AS payload
                  FROM user_mgmt.notification_outbox_event
                 WHERE status IN ('PENDING', 'FAILED')
                 ORDER BY created_at
                 LIMIT ?
                 FOR UPDATE SKIP LOCKED
                """,
                (rs, rowNum) -> new OutboxRow(
                        rs.getObject("id", UUID.class),
                        rs.getString("event_type"),
                        rs.getString("payload")
                ),
                batchSize
        );

        for (OutboxRow row : rows) {
            try {
                DomainNotificationEvent event = objectMapper.readValue(row.payload(), DomainNotificationEvent.class);
                rabbitTemplate.convertAndSend(exchange, routingKey, event);
                jdbcTemplate.update(
                        "UPDATE user_mgmt.notification_outbox_event SET status = 'PUBLISHED', published_at = now() WHERE id = ?",
                        row.id()
                );
            } catch (Exception ex) {
                jdbcTemplate.update(
                        "UPDATE user_mgmt.notification_outbox_event SET status = 'FAILED', published_at = NULL WHERE id = ?",
                        row.id()
                );
                log.warn("User notification outbox publish failed eventId={} eventType={}: {}",
                        row.id(), row.eventType(), ex.getMessage());
            }
        }
    }

    record DomainNotificationEvent(
            String eventId,
            String eventType,
            String tenantId,
            String recipientRef,
            String userId,
            String occurredAt,
            Map<String, Object> payload
    ) {
    }

    private record OutboxRow(UUID id, String eventType, String payload) {
    }
}
