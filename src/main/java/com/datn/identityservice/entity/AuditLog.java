package com.datn.identityservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuditLog {
    @Id
    UUID id;

    @Column(nullable = false, length = 100)
    String action;

    @Column(name = "actor_id", nullable = false)
    UUID actorId;

    @Column(name = "target_id")
    UUID targetId;

    String description;

    @Column(name = "ip_address", length = 50)
    String ipAddress;

    @Column(name = "user_agent")
    String userAgent;

    @Column(name = "created_at", insertable = false, updatable = false)
    Instant createdAt;
}
