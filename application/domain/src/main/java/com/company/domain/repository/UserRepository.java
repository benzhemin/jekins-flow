package com.company.domain.repository;

import com.company.domain.entity.User;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for User persistence operations
 *
 * This interface defines the contract for data access operations on Users.
 * The actual implementation is in the infrastructure module, allowing the
 * domain layer to remain independent of persistence details.
 *
 * This follows the Repository Pattern and DIP (Dependency Inversion Principle):
 * - High-level modules (domain) depend on abstractions (this interface)
 * - Low-level modules (infrastructure) implement the interface
 * - Neither depends on the other directly
 */
public interface UserRepository {

    /**
     * Save a user
     */
    User save(User user);

    /**
     * Find user by ID
     */
    Optional<User> findById(Long id);

    /**
     * Find user by email
     */
    Optional<User> findByEmail(String email);

    /**
     * Get all users
     */
    List<User> findAll();

    /**
     * Delete a user
     */
    void delete(User user);

    /**
     * Check if user exists by ID
     */
    boolean existsById(Long id);
}
