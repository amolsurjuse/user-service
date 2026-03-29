package com.electrahub.user.domain;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "terms_acceptance")
public class TermsAcceptance {
    private static final Logger LOGGER = LoggerFactory.getLogger(TermsAcceptance.class);

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "terms_version_id", nullable = false)
    private TermsVersion termsVersion;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "device_id", length = 255)
    private String deviceId;

    @Column(name = "device_platform", length = 20)
    private String devicePlatform;

    @Column(name = "device_model", length = 100)
    private String deviceModel;

    @Column(name = "app_version", length = 20)
    private String appVersion;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "accepted_at", nullable = false)
    private OffsetDateTime acceptedAt;

    @Column(name = "linked_at")
    private OffsetDateTime linkedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    /**
     * Executes constructor for `TermsAcceptance`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     */
    protected TermsAcceptance() {
        LOGGER.info("CODEx_ENTRY_LOG: Entering TermsAcceptance#TermsAcceptance");
        LOGGER.debug("CODEx_ENTRY_LOG: Entering TermsAcceptance#TermsAcceptance with debug context");
    }

    /**
     * Executes constructor for `TermsAcceptance`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @param id input consumed by TermsAcceptance.
     * @param termsVersion input consumed by TermsAcceptance.
     * @param acceptedAt input consumed by TermsAcceptance.
     */
    public TermsAcceptance(UUID id, TermsVersion termsVersion, OffsetDateTime acceptedAt) {
        this.id = id;
        this.termsVersion = termsVersion;
        this.acceptedAt = acceptedAt;
    }

    /**
     * Retrieves get id for `TermsAcceptance`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @return result produced by getId.
     */
    public UUID getId() {
        return id;
    }

    /**
     * Retrieves get terms version for `TermsAcceptance`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @return result produced by getTermsVersion.
     */
    public TermsVersion getTermsVersion() {
        return termsVersion;
    }

    /**
     * Retrieves get user id for `TermsAcceptance`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @return result produced by getUserId.
     */
    public UUID getUserId() {
        return userId;
    }

    /**
     * Retrieves get device id for `TermsAcceptance`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @return result produced by getDeviceId.
     */
    public String getDeviceId() {
        return deviceId;
    }

    /**
     * Retrieves get device platform for `TermsAcceptance`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @return result produced by getDevicePlatform.
     */
    public String getDevicePlatform() {
        return devicePlatform;
    }

    /**
     * Retrieves get device model for `TermsAcceptance`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @return result produced by getDeviceModel.
     */
    public String getDeviceModel() {
        return deviceModel;
    }

    /**
     * Retrieves get app version for `TermsAcceptance`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @return result produced by getAppVersion.
     */
    public String getAppVersion() {
        return appVersion;
    }

    /**
     * Retrieves get ip address for `TermsAcceptance`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @return result produced by getIpAddress.
     */
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * Retrieves get accepted at for `TermsAcceptance`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @return result produced by getAcceptedAt.
     */
    public OffsetDateTime getAcceptedAt() {
        return acceptedAt;
    }

    /**
     * Retrieves get linked at for `TermsAcceptance`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @return result produced by getLinkedAt.
     */
    public OffsetDateTime getLinkedAt() {
        return linkedAt;
    }

    /**
     * Retrieves get metadata for `TermsAcceptance`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @return result produced by getMetadata.
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    /**
     * Updates set user id for `TermsAcceptance`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @param userId input consumed by setUserId.
     */
    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    /**
     * Updates set device id for `TermsAcceptance`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @param deviceId input consumed by setDeviceId.
     */
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    /**
     * Updates set device platform for `TermsAcceptance`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @param devicePlatform input consumed by setDevicePlatform.
     */
    public void setDevicePlatform(String devicePlatform) {
        this.devicePlatform = devicePlatform;
    }

    /**
     * Updates set device model for `TermsAcceptance`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @param deviceModel input consumed by setDeviceModel.
     */
    public void setDeviceModel(String deviceModel) {
        this.deviceModel = deviceModel;
    }

    /**
     * Updates set app version for `TermsAcceptance`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @param appVersion input consumed by setAppVersion.
     */
    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    /**
     * Updates set ip address for `TermsAcceptance`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @param ipAddress input consumed by setIpAddress.
     */
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    /**
     * Updates set linked at for `TermsAcceptance`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @param linkedAt input consumed by setLinkedAt.
     */
    public void setLinkedAt(OffsetDateTime linkedAt) {
        this.linkedAt = linkedAt;
    }

    /**
     * Updates set metadata for `TermsAcceptance`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @param metadata input consumed by setMetadata.
     */
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
