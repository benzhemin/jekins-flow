package com.company.domain.service;

import com.company.common.dto.UserDTO;
import com.company.common.exception.ResourceNotFoundException;
import com.company.domain.entity.User;
import com.company.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Domain service for User business logic
 *
 * This service orchestrates domain entities and implements business rules.
 * It acts as a bridge between the REST API and the infrastructure/persistence layer.
 *
 * Note: The UserRepository interface is defined here (domain), but
 * the implementation is in the infrastructure module. This follows DIP
 * (Dependency Inversion Principle) - high-level modules depend on abstractions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * Create a new user
     *
     * @param userDTO the user data to create
     * @return the created user
     */
    public User createUser(UserDTO userDTO) {
        log.info("Creating user with email: {}", userDTO.getEmail());

        User user = User.builder()
            .name(userDTO.getName())
            .email(userDTO.getEmail())
            .active(true)
            .createdAt(System.currentTimeMillis())
            .build();

        User savedUser = userRepository.save(user);
        log.info("User created successfully with ID: {}", savedUser.getId());
        return savedUser;
    }

    /**
     * Retrieve a user by ID
     *
     * @param id the user ID
     * @return the user if found
     * @throws ResourceNotFoundException if user not found
     */
    public User getUserById(Long id) {
        log.debug("Fetching user with ID: {}", id);
        return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    /**
     * Get all users
     *
     * @return list of all users
     */
    public List<User> getAllUsers() {
        log.debug("Fetching all users");
        return userRepository.findAll();
    }

    /**
     * Find a user by email
     *
     * @param email the email to search
     * @return the user if found
     * @throws ResourceNotFoundException if user not found
     */
    public User getUserByEmail(String email) {
        log.debug("Fetching user with email: {}", email);
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User with email: " + email));
    }

    /**
     * Update a user
     *
     * @param id the user ID
     * @param userDTO the updated user data
     * @return the updated user
     */
    public User updateUser(Long id, UserDTO userDTO) {
        log.info("Updating user with ID: {}", id);
        User user = getUserById(id);
        user.setName(userDTO.getName());
        user.setEmail(userDTO.getEmail());
        if (userDTO.getActive() != null) {
            user.setActive(userDTO.getActive());
        }
        return userRepository.save(user);
    }

    /**
     * Delete a user
     *
     * @param id the user ID
     */
    public void deleteUser(Long id) {
        log.info("Deleting user with ID: {}", id);
        User user = getUserById(id);
        userRepository.delete(user);
    }

    /**
     * Deactivate a user account
     *
     * @param id the user ID
     * @return the deactivated user
     */
    public User deactivateUser(Long id) {
        log.info("Deactivating user with ID: {}", id);
        User user = getUserById(id);
        user.deactivateAccount();
        return userRepository.save(user);
    }
}
