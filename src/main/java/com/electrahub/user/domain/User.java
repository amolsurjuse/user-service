package com.electrahub.user.domain;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {
    private static final Logger LOGGER = LoggerFactory.getLogger(User.class);


    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 320)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "phone_number", length = 16)
    private String phoneNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id")
    private Address address;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "pending_deletion", nullable = false)
    private boolean pendingDeletion;

    @Column(name = "deletion_requested_at")
    private OffsetDateTime deletionRequestedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    /**
     * Executes user for `User`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     */
    protected User() {
        LOGGER.info("CODEx_ENTRY_LOG: Entering User#User");
        LOGGER.debug("CODEx_ENTRY_LOG: Entering User#User with debug context");
    }

    /**
     * Executes user for `User`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @param id input consumed by User.
     * @param email input consumed by User.
     * @param passwordHash input consumed by User.
     * @param enabled input consumed by User.
     * @param now input consumed by User.
     */
    public User(UUID id, String email, String passwordHash, boolean enabled, OffsetDateTime now) {
        this.id = id;
        this.email = email.toLowerCase();
        this.passwordHash = passwordHash;
        this.enabled = enabled;
        this.pendingDeletion = false;
        this.deletionRequestedAt = null;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * Executes pre update for `User`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     */
    @PreUpdate
    void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    /**
     * Retrieves get id for `User`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @return result produced by getId.
     */
    public UUID getId() {
        return id;
    }

    /**
     * Retrieves get email for `User`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @return result produced by getEmail.
     */
    public String getEmail() {
        return email;
    }

    /**
     * Retrieves get password hash for `User`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @return result produced by getPasswordHash.
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * Retrieves get first name for `User`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @return result produced by getFirstName.
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Retrieves get last name for `User`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @return result produced by getLastName.
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Retrieves get phone number for `User`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @return result produced by getPhoneNumber.
     */
    public String getPhoneNumber() {
        return phoneNumber;
    }

    /**
     * Retrieves get address for `User`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @return result produced by getAddress.
     */
    public Address getAddress() {
        return address;
    }

    /**
     * Executes is enabled for `User`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @return result produced by isEnabled.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Executes is pending deletion for `User`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @return result produced by isPendingDeletion.
     */
    public boolean isPendingDeletion() {
        return pendingDeletion;
    }

    /**
     * Retrieves get deletion requested at for `User`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @return result produced by getDeletionRequestedAt.
     */
    public OffsetDateTime getDeletionRequestedAt() {
        return deletionRequestedAt;
    }

    /**
     * Retrieves get created at for `User`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @return result produced by getCreatedAt.
     */
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Retrieves get updated at for `User`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @return result produced by getUpdatedAt.
     */
    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Retrieves get roles for `User`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @return result produced by getRoles.
     */
    public Set<Role> getRoles() {
        return roles;
    }

    /**
     * Creates add role for `User`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @param role input consumed by addRole.
     */
    public void addRole(Role role) {
        this.roles.add(role);
    }

    /**
     * Updates set first name for `User`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @param firstName input consumed by setFirstName.
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * Updates set last name for `User`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @param lastName input consumed by setLastName.
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     * Updates set phone number for `User`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @param phoneNumber input consumed by setPhoneNumber.
     */
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    /**
     * Updates set address for `User`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @param address input consumed by setAddress.
     */
    public void setAddress(Address address) {
        this.address = address;
    }

    /**
     * Updates set password hash for `User`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @param passwordHash input consumed by setPasswordHash.
     */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /**
     * Updates set enabled for `User`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @param enabled input consumed by setEnabled.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Updates set pending deletion for `User`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @param pendingDeletion input consumed by setPendingDeletion.
     */
    public void setPendingDeletion(boolean pendingDeletion) {
        this.pendingDeletion = pendingDeletion;
    }

    /**
     * Updates set deletion requested at for `User`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.domain`.
     * @param deletionRequestedAt input consumed by setDeletionRequestedAt.
     */
    public void setDeletionRequestedAt(OffsetDateTime deletionRequestedAt) {
        this.deletionRequestedAt = deletionRequestedAt;
    }
}
