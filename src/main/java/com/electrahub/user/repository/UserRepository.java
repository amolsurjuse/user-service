package com.electrahub.user.repository;

import com.electrahub.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

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
    Page<User> search(String query, Pageable pageable);

    @Query("""
            select count(u) from User u
            where (:query is null or :query = ''
                   or lower(u.email) like lower(concat('%', :query, '%'))
                   or lower(coalesce(u.firstName, '')) like lower(concat('%', :query, '%'))
                   or lower(coalesce(u.lastName, '')) like lower(concat('%', :query, '%'))
                   or lower(coalesce(u.phoneNumber, '')) like lower(concat('%', :query, '%')))
            """)
    long countSearch(String query);
}
