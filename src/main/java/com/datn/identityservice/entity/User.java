package com.datn.identityservice.entity;


import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User {
    @Id
    @GeneratedValue
    @org.hibernate.annotations.UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    UUID id;


    @Column(unique = true)
    String email;

    @Column(unique = true, length = 20)
    String phone;

    @Column(name = "password_hash")
    String passwordHash;

    @Column(nullable = false, length = 20)
    String status; // ACTIVE, BANNED, SUSPENDED, PENDING_DELETION, DELETED

    @Column(name = "two_factor_enabled")
    boolean twoFactorEnabled;

    @Column(name = "is_mfa_verified")
    boolean isMfaVerified;

    @Column(name = "failed_attempt_count")
    int failedAttemptCount;

    @Column(name = "locked_until")
    LocalDateTime lockedUntil;

    @Column(name = "last_login_at")
    Instant lastLoginAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    Instant createdAt;

    @Column(name = "updated_at")
    Instant updatedAt;

    @Column(name = "deleted_at")
    Instant deletedAt;

    @Column(name = "deleted_by_admin")
    boolean deletedByAdmin;

    @Column(name = "ban_reason")
    String banReason;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    Set<UserRole> userRoles;

    public void addRole(Role role) {
        if (userRoles == null) {
            userRoles = new HashSet<>();
        }
        UserRole userRole = UserRole.builder()
//                .id(new UserRoleId(this.id, role.getId()))
                .user(this)
                .role(role)
                .build();
        userRole.setId(new UserRoleId(this.id, role.getId()));
        userRoles.add(userRole);
    }
}