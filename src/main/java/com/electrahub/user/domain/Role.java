package com.electrahub.user.domain;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "roles")
public class Role {
    private static final Logger LOGGER = LoggerFactory.getLogger(Role.class);


    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 64)
    private String name;

    /**
     * Executes role for `Role`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     */
    protected Role() {
        LOGGER.info("CODEx_ENTRY_LOG: Entering Role#Role");
        LOGGER.debug("CODEx_ENTRY_LOG: Entering Role#Role with debug context");
    }

    /**
     * Executes role for `Role`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @param id input consumed by Role.
     * @param name input consumed by Role.
     */
    public Role(UUID id, String name) {
        this.id = id;
        this.name = name;
    }

    /**
     * Retrieves get id for `Role`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @return result produced by getId.
     */
    public UUID getId() {
        return id;
    }

    /**
     * Retrieves get name for `Role`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @return result produced by getName.
     */
    public String getName() {
        return name;
    }
}
