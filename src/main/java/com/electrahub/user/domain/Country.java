package com.electrahub.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "country")
public class Country {

    @Id
    private UUID id;

    @Column(name = "iso_code", nullable = false, unique = true, length = 8)
    private String isoCode;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "dial_code", nullable = false, length = 8)
    private String dialCode;

    @Column(nullable = false)
    private boolean enabled;

    protected Country() {
    }

    public UUID getId() {
        return id;
    }

    public String getIsoCode() {
        return isoCode;
    }

    public String getName() {
        return name;
    }

    public String getDialCode() {
        return dialCode;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
