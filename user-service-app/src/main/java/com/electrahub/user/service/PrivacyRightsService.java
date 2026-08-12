package com.electrahub.user.service;

import com.electrahub.user.api.dto.UserProfileResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class PrivacyRightsService {
    private static final Set<String> TYPES = Set.of("ACCESS", "ERASURE", "RESTRICTION", "RECTIFICATION", "PORTABILITY");
    private final JdbcTemplate jdbc;
    private final UserManagementService users;

    public PrivacyRightsService(JdbcTemplate jdbc, UserManagementService users) {
        this.jdbc = jdbc;
        this.users = users;
    }

    public Export export(UUID userId) {
        UserProfileResponse profile = users.getProfile(userId);
        List<RequestView> requests = list(userId);
        return new Export("1.0", OffsetDateTime.now(), profile, requests,
                List.of("profile", "address", "billing identity", "privacy request history"),
                "Charging, payment, and legal records are exported by their owning services through the coordinated DSR workflow.");
    }

    @Transactional
    public RequestView submit(UUID userId, Submit command) {
        users.getProfile(userId);
        String type = required(command.type(), 24).toUpperCase(Locale.ROOT);
        if (!TYPES.contains(type)) throw new IllegalArgumentException("Unsupported privacy request type");
        UUID id = UUID.randomUUID();
        OffsetDateTime submitted = OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        OffsetDateTime due = submitted.plusDays(30);
        jdbc.update("""
                INSERT INTO user_mgmt.privacy_requests
                  (id,user_id,request_type,status,jurisdiction_code,details,submitted_at,due_at)
                VALUES (?,?,?,'RECEIVED',?,?,?,?)
                """, id, userId, type, bounded(command.jurisdictionCode(), 20), bounded(command.details(), 1000), submitted, due);
        return new RequestView(id, type, "RECEIVED", bounded(command.jurisdictionCode(), 20), submitted, due);
    }

    public List<RequestView> list(UUID userId) {
        users.getProfile(userId);
        return jdbc.query("""
                SELECT r.id,r.request_type,COALESCE(e.resulting_status,r.status),r.jurisdiction_code,
                       r.submitted_at,r.due_at,e.event_type,e.reason_code,e.retention_until
                FROM user_mgmt.privacy_requests r
                LEFT JOIN LATERAL (
                  SELECT resulting_status,event_type,reason_code,retention_until
                    FROM user_mgmt.privacy_request_events
                   WHERE request_id=r.id ORDER BY occurred_at DESC LIMIT 1
                ) e ON true
                WHERE r.user_id=? ORDER BY r.submitted_at DESC
                """, (rs, row) -> new RequestView(rs.getObject(1, UUID.class), rs.getString(2), rs.getString(3),
                rs.getString(4), rs.getObject(5, OffsetDateTime.class), rs.getObject(6, OffsetDateTime.class),
                rs.getString(7), rs.getString(8), rs.getObject(9, OffsetDateTime.class)), userId);
    }

    @Transactional
    public RequestView process(UUID requestId, Process command) {
        String action = required(command.action(), 32).toUpperCase(Locale.ROOT);
        RequestState state = jdbc.query("""
                SELECT r.user_id,COALESCE(e.resulting_status,r.status)
                  FROM user_mgmt.privacy_requests r
                  LEFT JOIN LATERAL (
                    SELECT resulting_status FROM user_mgmt.privacy_request_events
                     WHERE request_id=r.id ORDER BY occurred_at DESC LIMIT 1
                  ) e ON true WHERE r.id=? FOR UPDATE OF r
                """, (rs, row) -> new RequestState(rs.getObject(1, UUID.class), rs.getString(2)), requestId)
                .stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Privacy request not found"));
        String resultingStatus = validateTransition(state.status(), action, command.legalBasis(),
                command.retentionUntil(), OffsetDateTime.now());
        jdbc.update("""
                INSERT INTO user_mgmt.privacy_request_events
                  (id,request_id,event_type,resulting_status,reason_code,legal_basis,retention_until,actor_id,occurred_at)
                VALUES (?,?,?,?,?,?,?,?,NOW())
                """, UUID.randomUUID(), requestId, action, resultingStatus, bounded(command.reasonCode(), 80),
                bounded(command.legalBasis(), 500), command.retentionUntil(), required(command.actorId(), 160));
        return listInternal(state.userId()).stream().filter(item -> item.id().equals(requestId)).findFirst().orElseThrow();
    }

    static String validateTransition(String currentStatus, String action, String legalBasis,
                                     OffsetDateTime retentionUntil, OffsetDateTime now) {
        if (!Set.of("REVIEW_STARTED", "LAWFUL_HOLD_APPLIED", "FULFILLED", "REJECTED").contains(action)) {
            throw new IllegalArgumentException("Unsupported privacy workflow action");
        }
        if (Set.of("FULFILLED", "REJECTED").contains(currentStatus)) {
            throw new IllegalStateException("Privacy request is already terminal");
        }
        if ((action.equals("FULFILLED") || action.equals("REJECTED") || action.equals("LAWFUL_HOLD_APPLIED"))
                && !currentStatus.equals("IN_REVIEW")) {
            throw new IllegalStateException("Privacy request must be in review first");
        }
        if (action.equals("LAWFUL_HOLD_APPLIED")
                && (legalBasis == null || legalBasis.isBlank() || retentionUntil == null
                || !retentionUntil.isAfter(now))) {
            throw new IllegalArgumentException("A future retentionUntil and legalBasis are required for a lawful hold");
        }
        return switch (action) {
            case "REVIEW_STARTED", "LAWFUL_HOLD_APPLIED" -> "IN_REVIEW";
            case "FULFILLED" -> "FULFILLED";
            case "REJECTED" -> "REJECTED";
            default -> throw new IllegalStateException("Unexpected action");
        };
    }

    private List<RequestView> listInternal(UUID userId) {
        return jdbc.query("""
                SELECT r.id,r.request_type,COALESCE(e.resulting_status,r.status),r.jurisdiction_code,
                       r.submitted_at,r.due_at,e.event_type,e.reason_code,e.retention_until
                FROM user_mgmt.privacy_requests r
                LEFT JOIN LATERAL (
                  SELECT resulting_status,event_type,reason_code,retention_until
                    FROM user_mgmt.privacy_request_events WHERE request_id=r.id ORDER BY occurred_at DESC LIMIT 1
                ) e ON true WHERE r.user_id=? ORDER BY r.submitted_at DESC
                """, (rs, row) -> new RequestView(rs.getObject(1, UUID.class), rs.getString(2), rs.getString(3),
                rs.getString(4), rs.getObject(5, OffsetDateTime.class), rs.getObject(6, OffsetDateTime.class),
                rs.getString(7), rs.getString(8), rs.getObject(9, OffsetDateTime.class)), userId);
    }

    private static String required(String value, int max) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("type is required");
        return bounded(value, max);
    }

    private static String bounded(String value, int max) {
        if (value == null || value.isBlank()) return null;
        String trimmed = value.trim();
        if (trimmed.length() > max) throw new IllegalArgumentException("value exceeds " + max + " characters");
        return trimmed;
    }

    public record Submit(String type, String jurisdictionCode, String details) {}
    public record RequestView(UUID id, String type, String status, String jurisdictionCode,
                              OffsetDateTime submittedAt, OffsetDateTime dueAt, String latestAction,
                              String reasonCode, OffsetDateTime retentionUntil) {
        public RequestView(UUID id, String type, String status, String jurisdictionCode,
                           OffsetDateTime submittedAt, OffsetDateTime dueAt) {
            this(id, type, status, jurisdictionCode, submittedAt, dueAt, null, null, null);
        }
    }
    public record Process(String action, String reasonCode, String legalBasis,
                          OffsetDateTime retentionUntil, String actorId) {}
    private record RequestState(UUID userId, String status) {}
    public record Export(String schemaVersion, OffsetDateTime generatedAt, UserProfileResponse profile,
                         List<RequestView> privacyRequests, List<String> includedCategories, String coordinationNotice) {}
}
