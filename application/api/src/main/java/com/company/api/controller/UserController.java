package com.company.api.controller;

import com.company.common.dto.UserDTO;
import com.company.domain.entity.User;
import com.company.domain.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for User endpoints
 *
 * This controller handles HTTP requests related to users.
 * It acts as the interface between HTTP and the domain service layer.
 *
 * Endpoints:
 * - GET /api/users: List all users
 * - GET /api/users/{id}: Get a specific user
 * - POST /api/users: Create a new user
 * - PUT /api/users/{id}: Update a user
 * - DELETE /api/users/{id}: Delete a user
 * - POST /api/users/{id}/deactivate: Deactivate a user
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Get all users
     *
     * @return list of all users
     */
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        log.info("GET /api/users - Fetching all users");
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * Get a user by ID
     *
     * @param id the user ID
     * @return the user if found
     */
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        log.info("GET /api/users/{} - Fetching user", id);
        User user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    /**
     * Create a new user
     *
     * @param userDTO the user data
     * @return the created user with 201 status
     */
    @PostMapping
    public ResponseEntity<User> createUser(@Valid @RequestBody UserDTO userDTO) {
        log.info("POST /api/users - Creating user: {}", userDTO.getEmail());
        User user = userService.createUser(userDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    /**
     * Update a user
     *
     * @param id the user ID
     * @param userDTO the updated user data
     * @return the updated user
     */
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(
        @PathVariable Long id,
        @Valid @RequestBody UserDTO userDTO) {
        log.info("PUT /api/users/{} - Updating user", id);
        User user = userService.updateUser(id, userDTO);
        return ResponseEntity.ok(user);
    }

    /**
     * Delete a user
     *
     * @param id the user ID
     * @return 204 No Content status
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        log.info("DELETE /api/users/{} - Deleting user", id);
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Deactivate a user account
     *
     * @param id the user ID
     * @return the deactivated user
     */
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<User> deactivateUser(@PathVariable Long id) {
        log.info("POST /api/users/{}/deactivate - Deactivating user", id);
        User user = userService.deactivateUser(id);
        return ResponseEntity.ok(user);
    }
}
