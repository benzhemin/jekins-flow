package com.company.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JPA Entity for User persistence
 *
 * This is the persistence layer representation of a User.
 * Note the separation from the domain User entity - this allows
 * the domain layer to remain independent of JPA/database concerns.
 *
 * The infrastructure module handles mapping between:
 * - UserEntity (JPA) ↔ User (domain) ↔ UserDTO (API)
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_email", columnList = "email", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {

    /**
     * Primary key
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * User's full name
     */
    @Column(nullable = false)
    private String name;

    /**
     * User's email (unique)
     */
    @Column(nullable = false, unique = true)
    private String email;

    /**
     * Account status
     */
    @Column(nullable = false)
    private Boolean active;

    /**
     * Account creation timestamp
     */
    @Column(nullable = false)
    private Long createdAt;

    /**
     * Convert to domain User entity
     */
    public com.company.domain.entity.User toDomainEntity() {
        return com.company.domain.entity.User.builder()
            .id(this.id)
            .name(this.name)
            .email(this.email)
            .active(this.active)
            .createdAt(this.createdAt)
            .build();
    }

    /**
     * Create from domain User entity
     */
    public static UserEntity fromDomainEntity(com.company.domain.entity.User user) {
        return UserEntity.builder()
            .id(user.getId())
            .name(user.getName())
            .email(user.getEmail())
            .active(user.getActive())
            .createdAt(user.getCreatedAt())
            .build();
    }
}
