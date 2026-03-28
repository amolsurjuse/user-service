package com.electrahub.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "terms_device_link")
public class TermsDeviceLink {

    @Id
    private UUID id;

    @Column(name = "device_id", nullable = false, length = 255)
    private String deviceId;

    @Column(name = "device_platform", nullable = false, length = 20)
    private String devicePlatform;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "linked_at")
    private OffsetDateTime linkedAt;

    protected TermsDeviceLink() {
    }

    public TermsDeviceLink(UUID id, String deviceId, String devicePlatform, UUID userId, OffsetDateTime linkedAt) {
        this.id = id;
        this.deviceId = deviceId;
        this.devicePlatform = devicePlatform;
        this.userId = userId;
        this.linkedAt = linkedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getDevicePlatform() {
        return devicePlatform;
    }

    public UUID getUserId() {
        return userId;
    }

    public OffsetDateTime getLinkedAt() {
        return linkedAt;
    }

    public void setLinkedAt(OffsetDateTime linkedAt) {
        this.linkedAt = linkedAt;
    }
}
