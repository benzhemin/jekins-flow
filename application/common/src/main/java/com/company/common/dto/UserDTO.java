package com.company.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Data Transfer Object for User information
 *
 * This DTO is used to transfer user data between API layers and service layers.
 * It includes validation annotations to ensure data integrity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

    /**
     * Unique identifier for the user
     */
    private Long id;

    /**
     * User's full name
     */
    @NotBlank(message = "Name cannot be blank")
    private String name;

    /**
     * User's email address
     */
    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email should be valid")
    private String email;

    /**
     * User's status (active/inactive)
     */
    private Boolean active;

    /**
     * User's creation timestamp
     */
    private Long createdAt;
}
