package com.datn.identityservice.service;

import com.datn.identityservice.dto.request.EmailRegisterRequest;
import com.datn.identityservice.entity.*;
import com.datn.identityservice.mapper.UserMapper;
import com.datn.identityservice.repository.RoleRepository;
import com.datn.identityservice.repository.UserRepository;
import com.datn.identityservice.repository.UserRoleRepository;
import com.datn.identityservice.repository.VerificationTokenRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AuthenticationService {
    UserRepository userRepository;
    RoleRepository roleRepository;
    UserRoleRepository userRoleRepository;
    VerificationTokenRepository tokenRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;
    JavaMailSender mailSender;

    @org.springframework.transaction.annotation.Transactional
    public void registerByEmail(EmailRegisterRequest request) {
        // 1. Kiểm tra mật khẩu khớp
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("PASSWORD_NOT_MATCH");
        }

        // 2. Kiểm tra Email (Sửa dùng findByEmail để lấy User object)
        Optional<User> existingUser = userRepository.findByEmail(request.getEmail());
        User user;

        if (existingUser.isPresent()) {
            user = existingUser.get();
            if ("ACTIVE".equals(user.getStatus())) {
                throw new RuntimeException("EMAIL_ALREADY_REGISTERED");
            }
            // Xóa sạch token cũ liên quan đến user này trước khi cấp cái mới
            tokenRepository.deleteByUser(user);
        } else {
            user = userMapper.toUser(request);
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            user.setStatus("PENDING");
            user = userRepository.save(user);

            Role customerRole = roleRepository.findByName("CUSTOMER")
                    .orElseThrow(() -> new RuntimeException("ROLE_NOT_FOUND"));

            UserRole userRole = new UserRole();
            userRole.setId(new UserRoleId(user.getId(), customerRole.getId()));
            userRole.setUser(user);
            userRole.setRole(customerRole);
            userRoleRepository.save(userRole);
        }

        // 3. Tạo Verification Token
        String tokenValue = UUID.randomUUID().toString();
        VerificationToken verificationToken = VerificationToken.builder()
                .token(tokenValue)
                .user(user)
                .expiryDate(LocalDateTime.now().plusHours(24))
                .build();
        tokenRepository.save(verificationToken);

        // 4. Gửi Email
        sendVerificationEmail(user.getEmail(), tokenValue);
    }

    @org.springframework.transaction.annotation.Transactional
    public void verifyEmail(String token) {
        VerificationToken vToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("INVALID_TOKEN"));

        if (vToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            tokenRepository.delete(vToken);
            throw new RuntimeException("TOKEN_EXPIRED");
        }

        User user = vToken.getUser();
        user.setStatus("ACTIVE");
        userRepository.save(user);

        tokenRepository.delete(vToken);
        log.info("User {} verified successfully", user.getEmail());
    }

    private void sendVerificationEmail(String email, String token) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Kích hoạt tài khoản Identity của bạn");
            String verifyUrl = "http://localhost:8080/api/v1/auth/verify?token=" + token;
            message.setText("Chào bạn, vui lòng click vào link sau để kích hoạt tài khoản: " + verifyUrl);

            mailSender.send(message);
        } catch (Exception e) {
            log.error("Lỗi gửi mail: ", e);
            // Quan trọng: Phải ném lỗi ra để Transaction @Transactional rollback lại dữ liệu đã lưu
            throw new RuntimeException("EMAIL_SEND_FAILED");
        }
    }
}
