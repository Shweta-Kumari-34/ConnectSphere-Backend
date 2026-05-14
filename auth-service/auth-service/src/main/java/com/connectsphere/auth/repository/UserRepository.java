
package com.connectsphere.auth.repository;

import java.util.List;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.connectsphere.auth.entity.User;

/*
 * Repository layer
 * ----------------
 * This interface handles all DB operations for User.
 *
 * JpaRepository already provides:
 * save(), findById(), findAll(), deleteById() etc.
 *
 * Below are custom methods needed for auth-service.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Find a user by email - used during login
    Optional<User> findByEmail(String email);

    // Find a user by username - useful for profile/search
    Optional<User> findByUsername(String username);

    // Check if email already exists - used during registration
    boolean existsByEmail(String email);

    // Check if username already exists - used during registration
    boolean existsByUsername(String username);

    // Search users by partial username - can be used later
    List<User> findByUsernameContaining(String username);

    // Find users by role (e.g. ADMIN, USER)
    List<User> findAllByRole(String role);

    // Search by username for the search feature
    List<User> findByUsernameContainingIgnoreCase(String username);

    // Search by full name for user discovery
    List<User> findByFullNameContainingIgnoreCase(String fullName);
}