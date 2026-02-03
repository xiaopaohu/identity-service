package com.datn.identityservice.service;

import com.datn.identityservice.configuration.AppUrlProperties;
import com.datn.identityservice.dto.request.EmailRegisterRequest;
import com.datn.identityservice.entity.Role;
import com.datn.identityservice.entity.User;
import com.datn.identityservice.entity.VerificationToken;
import com.datn.identityservice.event.RegistrationCompleteEvent;
import com.datn.identityservice.mapper.UserMapper;
import com.datn.identityservice.repository.RoleRepository;
import com.datn.identityservice.repository.UserRepository;
import com.datn.identityservice.repository.UserRoleRepository;
import com.datn.identityservice.repository.VerificationTokenRepository;
import com.datn.identityservice.validator.RegisterValidator;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AuthenticationService {
    AppUrlProperties appUrl;

    UserRepository userRepository;
    RoleRepository roleRepository;
    UserRoleRepository userRoleRepository;
    VerificationTokenRepository tokenRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;
    JavaMailSender mailSender;
    RegisterValidator registerValidator;
    ApplicationEventPublisher eventPublisher;

    @Transactional
    public void registerByEmail(EmailRegisterRequest request) {
        registerValidator.validate(request);

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("EMAIL_ALREADY_EXISTS");
        }

        User user = userMapper.toUser(request);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setStatus("UNVERIFIED");

        userRepository.save(user);

        Role role = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseThrow(() -> new RuntimeException("ROLE_NOT_FOUND"));
        user.addRole(role);

        String tokenValue = UUID.randomUUID().toString();
        VerificationToken verificationToken = VerificationToken.builder()
                .token(tokenValue)
                .user(user)
                .expiryDate(Instant.now().plus(Duration.ofHours(24)))
                .build();

        tokenRepository.save(verificationToken);

//        try {
//            senVerificationEmail(user.getEmail(), tokenValue);
//        } catch (Exception e) {
//            log.error("SEND EMAIL FAILED: {}", e.getMessage());
//            throw new RuntimeException("SEND_EMAIL_FAILED");
//        }

        log.info("Phát sự kiện đăng ký cho email: {}", user.getEmail());
        eventPublisher.publishEvent(new RegistrationCompleteEvent(user.getEmail(), tokenValue));
    }

    @Transactional
    public void verifyEmail(String token) {
        VerificationToken verificationToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("INVALID_TOKEN"));

        if (verificationToken.getExpiryDate().isBefore(Instant.now())) {
            tokenRepository.delete(verificationToken);
            throw new RuntimeException("TOKEN_EXPIRED");
        }

        User user = verificationToken.getUser();
        user.setStatus("ACTIVE");
        userRepository.save(user);
        tokenRepository.delete(verificationToken);

        log.info("Xác thực thành công cho: {}", user.getEmail());
    }


    private void senVerificationEmail(String email, String token) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(email);
        mailMessage.setSubject("Xác thực tài khoản của bạn");

        String verifyUrl = String.format("%s/identity/auth/verify?token=%s", appUrl.getBase(), token);
        mailMessage.setText("Vui lòng click vào link sau để kích hoạt tài khoản: " + verifyUrl);
        mailSender.send(mailMessage);
    }
}
