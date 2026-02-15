package com.company.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User domain entity
 *
 * This represents a User in the domain model. The entity contains
 * the core business logic related to users. Note that this is separate
 * from the persistence layer (JPA entity) which is in the infrastructure module.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    /**
     * User's unique identifier
     */
    private Long id;

    /**
     * User's full name
     */
    private String name;

    /**
     * User's email address
     */
    private String email;

    /**
     * User's account status
     */
    private Boolean active;

    /**
     * User's account creation timestamp (milliseconds since epoch)
     */
    private Long createdAt;

    /**
     * Business logic: Check if user's account is active
     */
    public boolean isAccountActive() {
        return active != null && active;
    }

    /**
     * Business logic: Deactivate the user account
     */
    public void deactivateAccount() {
        this.active = false;
    }

    /**
     * Business logic: Activate the user account
     */
    public void activateAccount() {
        this.active = true;
    }
}
