package com.company.api.controller;

import com.company.common.dto.UserDTO;
import com.company.domain.entity.User;
import com.company.domain.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for UserController
 *
 * Tests verify that the controller properly handles HTTP requests and
 * delegates to the service layer.
 */
@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

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
    void testGetAllUsers() {
        // Given
        List<User> users = Arrays.asList(testUser);
        when(userService.getAllUsers()).thenReturn(users);

        // When
        ResponseEntity<List<User>> response = userController.getAllUsers();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(userService).getAllUsers();
    }

    @Test
    void testGetUserById() {
        // Given
        when(userService.getUserById(1L)).thenReturn(testUser);

        // When
        ResponseEntity<User> response = userController.getUserById(1L);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("John Doe", response.getBody().getName());
        verify(userService).getUserById(1L);
    }

    @Test
    void testCreateUser() {
        // Given
        when(userService.createUser(any(UserDTO.class))).thenReturn(testUser);

        // When
        ResponseEntity<User> response = userController.createUser(testUserDTO);

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getId());
        verify(userService).createUser(any(UserDTO.class));
    }

    @Test
    void testUpdateUser() {
        // Given
        when(userService.updateUser(eq(1L), any(UserDTO.class))).thenReturn(testUser);

        // When
        ResponseEntity<User> response = userController.updateUser(1L, testUserDTO);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(userService).updateUser(eq(1L), any(UserDTO.class));
    }

    @Test
    void testDeleteUser() {
        // When
        ResponseEntity<Void> response = userController.deleteUser(1L);

        // Then
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(userService).deleteUser(1L);
    }

    @Test
    void testDeactivateUser() {
        // Given
        User deactivatedUser = User.builder()
            .id(1L)
            .name("John Doe")
            .email("john@example.com")
            .active(false)
            .createdAt(System.currentTimeMillis())
            .build();
        when(userService.deactivateUser(1L)).thenReturn(deactivatedUser);

        // When
        ResponseEntity<User> response = userController.deactivateUser(1L);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().getActive());
        verify(userService).deactivateUser(1L);
    }
}
