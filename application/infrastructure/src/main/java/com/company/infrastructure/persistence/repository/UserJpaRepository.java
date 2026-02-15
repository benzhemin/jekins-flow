package com.company.infrastructure.persistence.repository;

import com.company.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * JPA repository for UserEntity persistence operations
 *
 * Spring Data JPA handles the low-level CRUD operations.
 * This is wrapped by UserRepositoryImpl which implements the domain UserRepository.
 *
 * Separation of concerns:
 * - JpaRepository: Low-level database operations
 * - UserRepositoryImpl: Domain repository implementation with business logic
 * - UserRepository (domain): Interface that domain layer depends on
 */
@Repository
public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {

    /**
     * Find user by email
     */
    Optional<UserEntity> findByEmail(String email);

    /**
     * Check if user exists by email
     */
    boolean existsByEmail(String email);
}
