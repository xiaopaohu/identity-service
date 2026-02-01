package com.datn.identityservice.repository;

import com.datn.identityservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmailOrPhone(String email, String phone);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    // Truy vấn kèm Roles để nạp vào SecurityContext (Tránh LazyInitializationException)
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.userRoles ur LEFT JOIN FETCH ur.role WHERE u.id = :id")
    Optional<User> findByIdWithRoles(@Param("id") UUID id);

    // Tìm các tài khoản chờ xóa quá hạn (Ví dụ 7 ngày cho User, 30 ngày cho Admin)
    @Query("SELECT u FROM User u WHERE u.status = 'PENDING_DELETION' AND u.deletedAt <= :threshold")
    List<User> findExpiredSoftDeletedUsers(@Param("threshold") LocalDateTime threshold);
}