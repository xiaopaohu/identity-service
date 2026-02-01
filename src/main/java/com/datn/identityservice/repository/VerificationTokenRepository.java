package com.datn.identityservice.repository;

import com.datn.identityservice.entity.User;
import com.datn.identityservice.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    Optional<VerificationToken> findByToken(String token);

    void deleteByExpiryDateBefore(LocalDateTime now);

    void deleteByUser(User user); // Để dọn sạch nếu user đăng ký lại
}