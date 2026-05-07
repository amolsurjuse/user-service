package com.electrahub.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "terms_versions")
public class TermsVersion {

    @Id
    private UUID id;

    @Column(name = "version_number", nullable = false, unique = true)
    private int versionNumber;

    @Column(name = "version_label", nullable = false, length = 32)
    private String versionLabel;

    @Column(name = "content_url", nullable = false)
    private String contentUrl;

    @Column(name = "content_sha256", nullable = false, length = 64)
    private String contentSha256;

    @Column(name = "requires_re_acceptance", nullable = false)
    private boolean requiresReAcceptance;

    @Column(name = "effective_date", nullable = false)
    private OffsetDateTime effectiveDate;

    @Column(name = "published_by", nullable = false)
    private UUID publishedBy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    protected TermsVersion() {
    }

    public TermsVersion(
            UUID id,
            int versionNumber,
            String versionLabel,
            String contentUrl,
            String contentSha256,
            boolean requiresReAcceptance,
            OffsetDateTime effectiveDate,
            UUID publishedBy,
            OffsetDateTime createdAt
    ) {
        this.id = id;
        this.versionNumber = versionNumber;
        this.versionLabel = versionLabel;
        this.contentUrl = contentUrl;
        this.contentSha256 = contentSha256;
        this.requiresReAcceptance = requiresReAcceptance;
        this.effectiveDate = effectiveDate;
        this.publishedBy = publishedBy;
        this.createdAt = createdAt;
        this.active = false;
    }

    public UUID getId() {
        return id;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public String getVersionLabel() {
        return versionLabel;
    }

    public String getContentUrl() {
        return contentUrl;
    }

    public String getContentSha256() {
        return contentSha256;
    }

    public boolean isRequiresReAcceptance() {
        return requiresReAcceptance;
    }

    public OffsetDateTime getEffectiveDate() {
        return effectiveDate;
    }

    public UUID getPublishedBy() {
        return publishedBy;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isActive() {
        return active;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }
}
