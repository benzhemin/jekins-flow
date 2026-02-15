package com.company.domain.service;

import com.company.common.dto.UserDTO;
import com.company.common.exception.ResourceNotFoundException;
import com.company.domain.entity.User;
import com.company.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService
 *
 * Tests verify that the service properly implements business logic
 * for user management.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UserDTO testUserDTO;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
            .id(1L)
            .name("John Doe")
            .email("john@example.com")
            .active(true)
            .createdAt(System.currentTimeMillis())
            .build();

        testUserDTO = UserDTO.builder()
            .name("John Doe")
            .email("john@example.com")
            .active(true)
            .build();
    }

    @Test
    void testCreateUser() {
        // Given
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User createdUser = userService.createUser(testUserDTO);

        // Then
        assertNotNull(createdUser);
        assertEquals("john@example.com", createdUser.getEmail());
        assertTrue(createdUser.isAccountActive());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testGetUserById() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        User user = userService.getUserById(1L);

        // Then
        assertNotNull(user);
        assertEquals(1L, user.getId());
        verify(userRepository).findById(1L);
    }

    @Test
    void testGetUserByIdNotFound() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            userService.getUserById(999L);
        });
        verify(userRepository).findById(999L);
    }

    @Test
    void testGetAllUsers() {
        // Given
        List<User> users = Arrays.asList(testUser);
        when(userRepository.findAll()).thenReturn(users);

        // When
        List<User> result = userService.getAllUsers();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(userRepository).findAll();
    }

    @Test
    void testGetUserByEmail() {
        // Given
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));

        // When
        User user = userService.getUserByEmail("john@example.com");

        // Then
        assertNotNull(user);
        assertEquals("john@example.com", user.getEmail());
        verify(userRepository).findByEmail("john@example.com");
    }

    @Test
    void testUpdateUser() {
        // Given
        UserDTO updateDTO = UserDTO.builder()
            .name("Jane Doe")
            .email("jane@example.com")
            .active(true)
            .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User updatedUser = userService.updateUser(1L, updateDTO);

        // Then
        assertNotNull(updatedUser);
        verify(userRepository).findById(1L);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testDeleteUser() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        userService.deleteUser(1L);

        // Then
        verify(userRepository).findById(1L);
        verify(userRepository).delete(testUser);
    }

    @Test
    void testDeactivateUser() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User deactivatedUser = userService.deactivateUser(1L);

        // Then
        assertNotNull(deactivatedUser);
        verify(userRepository).findById(1L);
        verify(userRepository).save(any(User.class));
    }
}
