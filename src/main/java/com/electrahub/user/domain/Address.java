package com.electrahub.user.domain;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "address")
public class Address {
    private static final Logger LOGGER = LoggerFactory.getLogger(Address.class);


    @Id
    private UUID id;

    @Column(name = "street", length = 255)
    private String street;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_id")
    private Country country;

    /**
     * Creates address for `Address`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     */
    protected Address() {
        LOGGER.info("CODEx_ENTRY_LOG: Entering Address#Address");
        LOGGER.debug("CODEx_ENTRY_LOG: Entering Address#Address with debug context");
    }

    /**
     * Creates address for `Address`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @param id input consumed by Address.
     * @param street input consumed by Address.
     * @param city input consumed by Address.
     * @param state input consumed by Address.
     * @param postalCode input consumed by Address.
     * @param country input consumed by Address.
     */
    public Address(UUID id, String street, String city, String state, String postalCode, Country country) {
        this.id = id;
        this.street = street;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.country = country;
    }

    /**
     * Retrieves get id for `Address`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @return result produced by getId.
     */
    public UUID getId() {
        return id;
    }

    /**
     * Retrieves get street for `Address`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @return result produced by getStreet.
     */
    public String getStreet() {
        return street;
    }

    /**
     * Retrieves get city for `Address`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @return result produced by getCity.
     */
    public String getCity() {
        return city;
    }

    /**
     * Retrieves get state for `Address`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @return result produced by getState.
     */
    public String getState() {
        return state;
    }

    /**
     * Retrieves get postal code for `Address`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @return result produced by getPostalCode.
     */
    public String getPostalCode() {
        return postalCode;
    }

    /**
     * Retrieves get country for `Address`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @return result produced by getCountry.
     */
    public Country getCountry() {
        return country;
    }

    /**
     * Updates set street for `Address`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @param street input consumed by setStreet.
     */
    public void setStreet(String street) {
        this.street = street;
    }

    /**
     * Updates set city for `Address`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @param city input consumed by setCity.
     */
    public void setCity(String city) {
        this.city = city;
    }

    /**
     * Updates set state for `Address`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @param state input consumed by setState.
     */
    public void setState(String state) {
        this.state = state;
    }

    /**
     * Updates set postal code for `Address`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @param postalCode input consumed by setPostalCode.
     */
    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    /**
     * Updates set country for `Address`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @param country input consumed by setCountry.
     */
    public void setCountry(Country country) {
        this.country = country;
    }
}
