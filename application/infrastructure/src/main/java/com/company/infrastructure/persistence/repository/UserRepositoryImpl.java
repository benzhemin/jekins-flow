package com.company.infrastructure.persistence.repository;

import com.company.domain.entity.User;
import com.company.domain.repository.UserRepository;
import com.company.infrastructure.persistence.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of UserRepository interface
 *
 * This component bridges the gap between the domain layer (UserRepository interface)
 * and the persistence layer (UserJpaRepository and database).
 *
 * Responsibilities:
 * - Convert between domain User and JPA UserEntity
 * - Delegate to UserJpaRepository for actual database operations
 * - Add any repository-level business logic
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository userJpaRepository;

    @Override
    public User save(User user) {
        log.debug("Saving user: {}", user.getEmail());
        UserEntity entity = UserEntity.fromDomainEntity(user);
        UserEntity saved = userJpaRepository.save(entity);
        return saved.toDomainEntity();
    }

    @Override
    public Optional<User> findById(Long id) {
        log.debug("Finding user by ID: {}", id);
        return userJpaRepository.findById(id)
            .map(UserEntity::toDomainEntity);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        log.debug("Finding user by email: {}", email);
        return userJpaRepository.findByEmail(email)
            .map(UserEntity::toDomainEntity);
    }

    @Override
    public List<User> findAll() {
        log.debug("Finding all users");
        return userJpaRepository.findAll().stream()
            .map(UserEntity::toDomainEntity)
            .toList();
    }

    @Override
    public void delete(User user) {
        log.debug("Deleting user: {}", user.getId());
        UserEntity entity = UserEntity.fromDomainEntity(user);
        userJpaRepository.delete(entity);
    }

    @Override
    public boolean existsById(Long id) {
        return userJpaRepository.existsById(id);
    }
}
