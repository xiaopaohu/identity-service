package com.datn.identityservice.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RefreshToken {
    @Id
    @GeneratedValue
    @org.hibernate.annotations.UuidGenerator
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Column(unique = true, nullable = false)
    String token;

    @Column(name = "device_info")
    String deviceInfo;

    @Column(name = "ip_address", length = 50)
    String ipAddress;

    @Column(name = "expires_at", nullable = false)
    Instant expiresAt;

    boolean revoked;

    @Column(name = "created_at", insertable = false, updatable = false)
    Instant createdAt;
}