package com.datn.identityservice.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminUserUpdateRequest {
    String password;
    String status;
    Boolean twoFactorEnabled;
    Boolean isMfaVerified;
    Integer failedAttemptCount;
    Instant lockedUntil;
    String banReason;
    Set<String> roles;
}
