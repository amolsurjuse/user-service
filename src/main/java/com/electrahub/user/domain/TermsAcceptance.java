package com.electrahub.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "terms_acceptances")
public class TermsAcceptance {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "terms_version_id", nullable = false)
    private TermsVersion termsVersion;

    @Column(name = "accepted_at", nullable = false)
    private OffsetDateTime acceptedAt;

    @Column(name = "device_id", nullable = false, length = 128)
    private String deviceId;

    @Column(name = "device_model", nullable = false, length = 64)
    private String deviceModel;

    @Column(name = "os_version", nullable = false, length = 32)
    private String osVersion;

    @Column(name = "app_version", nullable = false, length = 32)
    private String appVersion;

    @Column(nullable = false, length = 16)
    private String platform;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @Column(name = "user_agent", nullable = false)
    private String userAgent;

    protected TermsAcceptance() {
    }

    public TermsAcceptance(
            UUID id,
            UUID userId,
            TermsVersion termsVersion,
            OffsetDateTime acceptedAt,
            String deviceId,
            String deviceModel,
            String osVersion,
            String appVersion,
            String platform,
            String ipAddress,
            String userAgent
    ) {
        this.id = id;
        this.userId = userId;
        this.termsVersion = termsVersion;
        this.acceptedAt = acceptedAt;
        this.deviceId = deviceId;
        this.deviceModel = deviceModel;
        this.osVersion = osVersion;
        this.appVersion = appVersion;
        this.platform = platform;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public TermsVersion getTermsVersion() {
        return termsVersion;
    }

    public OffsetDateTime getAcceptedAt() {
        return acceptedAt;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getDeviceModel() {
        return deviceModel;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public String getPlatform() {
        return platform;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }
}
