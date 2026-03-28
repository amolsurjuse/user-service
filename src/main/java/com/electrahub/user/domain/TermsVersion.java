package com.electrahub.user.domain;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "terms_version")
public class TermsVersion {
    private static final Logger LOGGER = LoggerFactory.getLogger(TermsVersion.class);

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 20)
    private String version;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "content_url", nullable = false)
    private String contentUrl;

    @Column
    private String summary;

    @Column(name = "effective_date", nullable = false)
    private OffsetDateTime effectiveDate;

    @Column(name = "enforcement_mode", nullable = false, length = 20)
    private String enforcementMode;

    @Column(name = "min_app_version_ios", length = 20)
    private String minAppVersionIos;

    @Column(name = "min_app_version_android", length = 20)
    private String minAppVersionAndroid;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "created_by", nullable = false, length = 255)
    private String createdBy;

    /**
     * Executes constructor for `TermsVersion`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     */
    protected TermsVersion() {
        LOGGER.info("CODEx_ENTRY_LOG: Entering TermsVersion#TermsVersion");
        LOGGER.debug("CODEx_ENTRY_LOG: Entering TermsVersion#TermsVersion with debug context");
    }

    /**
     * Executes constructor for `TermsVersion`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @param id input consumed by TermsVersion.
     * @param version input consumed by TermsVersion.
     * @param title input consumed by TermsVersion.
     * @param contentUrl input consumed by TermsVersion.
     * @param effectiveDate input consumed by TermsVersion.
     * @param enforcementMode input consumed by TermsVersion.
     * @param isActive input consumed by TermsVersion.
     * @param createdAt input consumed by TermsVersion.
     * @param createdBy input consumed by TermsVersion.
     */
    public TermsVersion(UUID id, String version, String title, String contentUrl, OffsetDateTime effectiveDate, String enforcementMode, boolean isActive, OffsetDateTime createdAt, String createdBy) {
        this.id = id;
        this.version = version;
        this.title = title;
        this.contentUrl = contentUrl;
        this.effectiveDate = effectiveDate;
        this.enforcementMode = enforcementMode;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
    }

    /**
     * Retrieves get id for `TermsVersion`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @return result produced by getId.
     */
    public UUID getId() {
        return id;
    }

    /**
     * Retrieves get version for `TermsVersion`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @return result produced by getVersion.
     */
    public String getVersion() {
        return version;
    }

    /**
     * Retrieves get title for `TermsVersion`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @return result produced by getTitle.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Retrieves get content url for `TermsVersion`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @return result produced by getContentUrl.
     */
    public String getContentUrl() {
        return contentUrl;
    }

    /**
     * Retrieves get summary for `TermsVersion`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @return result produced by getSummary.
     */
    public String getSummary() {
        return summary;
    }

    /**
     * Retrieves get effective date for `TermsVersion`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @return result produced by getEffectiveDate.
     */
    public OffsetDateTime getEffectiveDate() {
        return effectiveDate;
    }

    /**
     * Retrieves get enforcement mode for `TermsVersion`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @return result produced by getEnforcementMode.
     */
    public String getEnforcementMode() {
        return enforcementMode;
    }

    /**
     * Retrieves get min app version ios for `TermsVersion`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @return result produced by getMinAppVersionIos.
     */
    public String getMinAppVersionIos() {
        return minAppVersionIos;
    }

    /**
     * Retrieves get min app version android for `TermsVersion`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @return result produced by getMinAppVersionAndroid.
     */
    public String getMinAppVersionAndroid() {
        return minAppVersionAndroid;
    }

    /**
     * Retrieves is active for `TermsVersion`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @return result produced by isActive.
     */
    public boolean isActive() {
        return isActive;
    }

    /**
     * Retrieves get metadata for `TermsVersion`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @return result produced by getMetadata.
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    /**
     * Retrieves get created at for `TermsVersion`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @return result produced by getCreatedAt.
     */
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Retrieves get created by for `TermsVersion`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @return result produced by getCreatedBy.
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * Updates set summary for `TermsVersion`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @param summary input consumed by setSummary.
     */
    public void setSummary(String summary) {
        this.summary = summary;
    }

    /**
     * Updates set min app version ios for `TermsVersion`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @param minAppVersionIos input consumed by setMinAppVersionIos.
     */
    public void setMinAppVersionIos(String minAppVersionIos) {
        this.minAppVersionIos = minAppVersionIos;
    }

    /**
     * Updates set min app version android for `TermsVersion`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @param minAppVersionAndroid input consumed by setMinAppVersionAndroid.
     */
    public void setMinAppVersionAndroid(String minAppVersionAndroid) {
        this.minAppVersionAndroid = minAppVersionAndroid;
    }

    /**
     * Updates set is active for `TermsVersion`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @param isActive input consumed by setIsActive.
     */
    public void setIsActive(boolean isActive) {
        this.isActive = isActive;
    }

    /**
     * Updates set metadata for `TermsVersion`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @param metadata input consumed by setMetadata.
     */
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
