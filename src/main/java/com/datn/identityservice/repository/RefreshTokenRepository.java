package com.datn.identityservice.repository;

import com.datn.identityservice.entity.RefreshToken;
import com.datn.identityservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByToken(String token);

    void deleteByUser(User user);

    // Lấy danh sách thiết bị đang đăng nhập của User
    List<RefreshToken> findAllByUserIdAndRevokedFalseAndExpiresAtAfter(UUID userId, LocalDateTime now);

    // Thu hồi toàn bộ session của một "victim"
    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.user.id = :userId")
    void revokeAllByUserId(@Param("userId") UUID userId);
}