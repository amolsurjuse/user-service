package com.electrahub.user.repository;

import com.electrahub.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Retrieves find by email for `UserRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.repository`.
     * @param email input consumed by findByEmail.
     * @return result produced by findByEmail.
     */
    Optional<User> findByEmail(String email);

    /**
     * Executes exists by email for `UserRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.repository`.
     * @param email input consumed by existsByEmail.
     * @return result produced by existsByEmail.
     */
    boolean existsByEmail(String email);

    @Query("""
            select u from User u
            where (:query is null or :query = ''
                   or lower(u.email) like lower(concat('%', :query, '%'))
                   or lower(coalesce(u.firstName, '')) like lower(concat('%', :query, '%'))
                   or lower(coalesce(u.lastName, '')) like lower(concat('%', :query, '%'))
                   or lower(coalesce(u.phoneNumber, '')) like lower(concat('%', :query, '%')))
            order by u.createdAt desc
            """)
    /**
     * Executes search for `UserRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.repository`.
     * @param query input consumed by search.
     * @param pageable input consumed by search.
     * @return result produced by search.
     */
    Page<User> search(String query, Pageable pageable);

    @Query("""
            select distinct u from User u
            where (:query is null or :query = ''
                   or lower(u.email) like lower(concat('%', :query, '%'))
                   or lower(coalesce(u.firstName, '')) like lower(concat('%', :query, '%'))
                   or lower(coalesce(u.lastName, '')) like lower(concat('%', :query, '%'))
                   or lower(coalesce(u.phoneNumber, '')) like lower(concat('%', :query, '%')))
              and u.id not in (
                   select adminUser.id
                   from User adminUser
                   join adminUser.roles adminRole
                   where adminRole.name = 'SYSTEM_ADMIN'
              )
            order by u.createdAt desc
            """)
    /**
     * Executes search regular users for `UserRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.repository`.
     * @param query input consumed by searchRegularUsers.
     * @param pageable input consumed by searchRegularUsers.
     * @return result produced by searchRegularUsers.
     */
    Page<User> searchRegularUsers(String query, Pageable pageable);

    @Query("""
            select distinct u from User u
            join u.roles role
            where role.name = 'SYSTEM_ADMIN'
              and (:query is null or :query = ''
                   or lower(u.email) like lower(concat('%', :query, '%'))
                   or lower(coalesce(u.firstName, '')) like lower(concat('%', :query, '%'))
                   or lower(coalesce(u.lastName, '')) like lower(concat('%', :query, '%'))
                   or lower(coalesce(u.phoneNumber, '')) like lower(concat('%', :query, '%')))
            order by u.createdAt desc
            """)
    /**
     * Executes search system admins for `UserRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.repository`.
     * @param query input consumed by searchSystemAdmins.
     * @param pageable input consumed by searchSystemAdmins.
     * @return result produced by searchSystemAdmins.
     */
    Page<User> searchSystemAdmins(String query, Pageable pageable);

    @Query("""
            select count(u) from User u
            where (:query is null or :query = ''
                   or lower(u.email) like lower(concat('%', :query, '%'))
                   or lower(coalesce(u.firstName, '')) like lower(concat('%', :query, '%'))
                   or lower(coalesce(u.lastName, '')) like lower(concat('%', :query, '%'))
                   or lower(coalesce(u.phoneNumber, '')) like lower(concat('%', :query, '%')))
            """)
    /**
     * Executes count search for `UserRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.repository`.
     * @param query input consumed by countSearch.
     * @return result produced by countSearch.
     */
    long countSearch(String query);

    @Query("""
            select count(u) from User u
            where (:query is null or :query = ''
                   or lower(u.email) like lower(concat('%', :query, '%'))
                   or lower(coalesce(u.firstName, '')) like lower(concat('%', :query, '%'))
                   or lower(coalesce(u.lastName, '')) like lower(concat('%', :query, '%'))
                   or lower(coalesce(u.phoneNumber, '')) like lower(concat('%', :query, '%')))
              and u.id not in (
                   select adminUser.id
                   from User adminUser
                   join adminUser.roles adminRole
                   where adminRole.name = 'SYSTEM_ADMIN'
              )
            """)
    /**
     * Executes count search regular users for `UserRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.repository`.
     * @param query input consumed by countSearchRegularUsers.
     * @return result produced by countSearchRegularUsers.
     */
    long countSearchRegularUsers(String query);

    @Query("""
            select count(distinct u) from User u
            join u.roles role
            where role.name = 'SYSTEM_ADMIN'
              and (:query is null or :query = ''
                   or lower(u.email) like lower(concat('%', :query, '%'))
                   or lower(coalesce(u.firstName, '')) like lower(concat('%', :query, '%'))
                   or lower(coalesce(u.lastName, '')) like lower(concat('%', :query, '%'))
                   or lower(coalesce(u.phoneNumber, '')) like lower(concat('%', :query, '%')))
            """)
    /**
     * Executes count search system admins for `UserRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.repository`.
     * @param query input consumed by countSearchSystemAdmins.
     * @return result produced by countSearchSystemAdmins.
     */
    long countSearchSystemAdmins(String query);
}
