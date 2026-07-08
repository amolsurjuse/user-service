package com.electrahub.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_charger_preferences")
public class UserChargerPreference {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "station_id", nullable = false, length = 128)
    private String stationId;

    @Column(name = "station_name", nullable = false, length = 255)
    private String stationName;

    @Column(length = 512)
    private String address;

    @Column(length = 128)
    private String city;

    @Column(length = 64)
    private String state;

    @Column(name = "postal_code", length = 32)
    private String postalCode;

    @Column(precision = 11, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 11, scale = 7)
    private BigDecimal longitude;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "operating_hours", length = 128)
    private String operatingHours;

    @Column(name = "image_url", length = 1024)
    private String imageUrl;

    @Column(name = "charger_ids_json", nullable = false, columnDefinition = "text")
    private String chargerIdsJson;

    @Column(name = "connectors_json", nullable = false, columnDefinition = "text")
    private String connectorsJson;

    @Column(nullable = false)
    private boolean favorite;

    @Column(name = "last_used_at")
    private OffsetDateTime lastUsedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected UserChargerPreference() {
    }

    public UserChargerPreference(UUID id, UUID userId, String stationId) {
        this.id = id;
        this.userId = userId;
        this.stationId = stationId;
    }

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getStationId() {
        return stationId;
    }

    public String getStationName() {
        return stationName;
    }

    public String getAddress() {
        return address;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public String getStatus() {
        return status;
    }

    public String getOperatingHours() {
        return operatingHours;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getChargerIdsJson() {
        return chargerIdsJson;
    }

    public String getConnectorsJson() {
        return connectorsJson;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public OffsetDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    public void markRecentlyUsed() {
        this.lastUsedAt = OffsetDateTime.now();
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    public void updateStationSnapshot(
            String stationName,
            String address,
            String city,
            String state,
            String postalCode,
            BigDecimal latitude,
            BigDecimal longitude,
            String status,
            String operatingHours,
            String imageUrl,
            String chargerIdsJson,
            String connectorsJson
    ) {
        this.stationName = stationName;
        this.address = address;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.latitude = latitude;
        this.longitude = longitude;
        this.status = status;
        this.operatingHours = operatingHours;
        this.imageUrl = imageUrl;
        this.chargerIdsJson = chargerIdsJson;
        this.connectorsJson = connectorsJson;
    }
}
