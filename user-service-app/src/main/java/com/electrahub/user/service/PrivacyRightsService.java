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
                SELECT id,request_type,status,jurisdiction_code,submitted_at,due_at
                FROM user_mgmt.privacy_requests WHERE user_id=? ORDER BY submitted_at DESC
                """, (rs, row) -> new RequestView(rs.getObject(1, UUID.class), rs.getString(2), rs.getString(3),
                rs.getString(4), rs.getObject(5, OffsetDateTime.class), rs.getObject(6, OffsetDateTime.class)), userId);
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
                              OffsetDateTime submittedAt, OffsetDateTime dueAt) {}
    public record Export(String schemaVersion, OffsetDateTime generatedAt, UserProfileResponse profile,
                         List<RequestView> privacyRequests, List<String> includedCategories, String coordinationNotice) {}
}
