package com.datn.identityservice.repository;

import com.datn.identityservice.entity.User;
import com.datn.identityservice.entity.VerificationOtpCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface VerificationOtpCodeRepository extends JpaRepository<VerificationOtpCode, Long> {

    Optional<VerificationOtpCode> findTopByUserAndIsUsedFalseOrderByCreatedAtDesc(User user);

    Optional<VerificationOtpCode> findTopByUserOrderByCreatedAtDesc(User user);

    // 2. Tìm OTP hợp lệ theo mã OTP + user (dùng khi verify)
    // Chỉ lấy OTP còn hạn, chưa dùng, chưa bị khóa
    Optional<VerificationOtpCode> findByOtpCodeAndUserAndExpiryDateAfterAndIsUsedFalseAndLockedUntilLessThan(
            String otpCode,
            User user,
            Instant now,
            Instant nowForLock
    );

    boolean existsByUserAndExpiryDateAfterAndIsUsedFalse(User user, Instant now);

    @Modifying
    void deleteByUser(User user);
}
