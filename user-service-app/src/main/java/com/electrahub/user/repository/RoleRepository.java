package com.electrahub.user.repository;

import com.electrahub.user.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {
    /**
     * Retrieves find by name for `RoleRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.repository`.
     * @param name input consumed by findByName.
     * @return result produced by findByName.
     */
    Optional<Role> findByName(String name);
    /**
     * Retrieves find by name in for `RoleRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.repository`.
     * @param names input consumed by findByNameIn.
     * @return result produced by findByNameIn.
     */
    List<Role> findByNameIn(Collection<String> names);
}
