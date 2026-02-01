package com.datn.identityservice.entity;


import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
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
    LocalDateTime lastLoginAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    LocalDateTime deletedAt;

    @Column(name = "deleted_by_admin")
    boolean deletedByAdmin;

    @Column(name = "ban_reason")
    String banReason;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    Set<UserRole> userRoles;
}