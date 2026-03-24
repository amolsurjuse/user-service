package com.electrahub.user.domain;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "country")
public class Country {
    private static final Logger LOGGER = LoggerFactory.getLogger(Country.class);


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

    /**
     * Executes country for `Country`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     */
    protected Country() {
        LOGGER.info("CODEx_ENTRY_LOG: Entering Country#Country");
        LOGGER.debug("CODEx_ENTRY_LOG: Entering Country#Country with debug context");
    }

    /**
     * Retrieves get id for `Country`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @return result produced by getId.
     */
    public UUID getId() {
        return id;
    }

    /**
     * Retrieves get iso code for `Country`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @return result produced by getIsoCode.
     */
    public String getIsoCode() {
        return isoCode;
    }

    /**
     * Retrieves get name for `Country`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @return result produced by getName.
     */
    public String getName() {
        return name;
    }

    /**
     * Retrieves get dial code for `Country`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @return result produced by getDialCode.
     */
    public String getDialCode() {
        return dialCode;
    }

    /**
     * Executes is enabled for `Country`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @return result produced by isEnabled.
     */
    public boolean isEnabled() {
        return enabled;
    }
}
