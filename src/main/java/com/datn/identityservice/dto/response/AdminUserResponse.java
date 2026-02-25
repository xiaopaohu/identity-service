package com.datn.identityservice.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminUserResponse extends UserResponse {
    Integer failedAttemptCount;
    Instant lockedUntil;
    String banReason;
    Instant createdAt;
    Instant updatedAt;
    Boolean twoFactorEnabled;
}
