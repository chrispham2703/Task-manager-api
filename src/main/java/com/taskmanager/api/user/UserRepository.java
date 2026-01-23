package com.taskmanager.api.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    @Query("SELECT u FROM User u WHERE lower(u.email) = lower(:email)")
    Optional<User> findByEmail(@Param("email") String email);

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE lower(u.email) = lower(:email)")
    boolean existsByEmail(@Param("email") String email);

    @Query("SELECT u FROM User u WHERE lower(u.email) = lower(:email) AND u.status = 'ACTIVE'")
    Optional<User> findActiveByEmail(@Param("email") String email);
}
