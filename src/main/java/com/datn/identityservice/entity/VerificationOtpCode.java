package com.datn.identityservice.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Entity
@Table(name = "otp_tokens")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VerificationOtpCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Column(name = "otp_code", nullable = false, length = 6)
    String otpCode;

    @Column(name = "expiry_date", nullable = false)
    Instant expiryDate;

    @Column(name = "created_at", insertable = false, updatable = false)
    Instant createdAt;

    @Builder.Default
    @Column(name = "attempt_count")
    int attemptCount = 0;

    @Column(name = "last_attempt_at")
    Instant lastAttemptAt;

    @Builder.Default
    @Column(name = "resend_count")
    int resendCount = 0;

    @Column(name = "last_resend_at")
    Instant lastResendAt;

    @Builder.Default
    @Column(name = "is_used")
    boolean isUsed = false;

    @Column(name = "locked_until")
    Instant lockedUntil;
}
