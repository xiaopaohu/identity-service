package com.datn.identityservice.service.Impl;

import com.datn.identityservice.dto.event.RegistrationCompleteEvent;
import com.datn.identityservice.dto.event.SmsOtpEvent;
import com.datn.identityservice.dto.request.EmailRegisterRequest;
import com.datn.identityservice.dto.request.PhoneRegisterRequest;
import com.datn.identityservice.entity.Role;
import com.datn.identityservice.entity.User;
import com.datn.identityservice.entity.VerificationOtpCode;
import com.datn.identityservice.entity.VerificationToken;
import com.datn.identityservice.mapper.UserMapper;
import com.datn.identityservice.repository.RoleRepository;
import com.datn.identityservice.repository.UserRepository;
import com.datn.identityservice.repository.VerificationOtpCodeRepository;
import com.datn.identityservice.repository.VerificationTokenRepository;
import com.datn.identityservice.service.AuthenticationService;
import com.datn.identityservice.service.OtpLockoutManager;
import com.datn.identityservice.validator.RegisterValidator;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AuthenticationServiceImpl implements AuthenticationService {
    //    AppUrlProperties appUrl;

    UserRepository userRepository;
    RoleRepository roleRepository;
    //    UserRoleRepository userRoleRepository;
    VerificationTokenRepository tokenRepository;
    VerificationOtpCodeRepository verificationOtpCodeRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;
    //    JavaMailSender mailSender;
    RegisterValidator registerValidator;
    ApplicationEventPublisher eventPublisher;
    //    TelegramSmsService telegramSmsService;
    OtpLockoutManager otpLockoutManager;

    @Override
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

        String token = generateAndSaveToken(user);

        log.info("The event for sending an verify link to the email {} has been triggered!", user.getEmail());
        eventPublisher.publishEvent(new RegistrationCompleteEvent(user.getEmail(), token));

    }

    @Override
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

        log.info("Email verified successfully for user: {}, email: {}", user.getId(), user.getEmail());
    }

    @Override
    @Transactional
    public void registerByPhone(PhoneRegisterRequest request) {
        registerValidator.validate(request);

        if (userRepository.existsByPhone(request.getPhone())) {
            throw new RuntimeException("PHONE_ALREADY_EXISTS");
        }

        User user = userMapper.toUser(request);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setStatus("UNVERIFIED");
        userRepository.save(user);

        Role role = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseThrow(() -> new RuntimeException("ROLE_NOT_FOUND"));
        user.addRole(role);

        String otpCode = generateAndSaveOtp(user);

        log.info("The event for sending an OTP to the phone {} has been triggered!", user.getPhone());
        eventPublisher.publishEvent(new SmsOtpEvent(request.getPhone(), otpCode));
    }

    @Override
    @Transactional
    public void verifyPhone(String phone, String otpCode) {
//        String normalizedPhone = normalizePhone(phone);

        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("USER_NOT_REGISTERED"));

        Instant now = Instant.now();

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
            long minutesLeft = Duration.between(now, user.getLockedUntil()).toMinutes();
            throw new RuntimeException("ACCOUNT_LOCKED_PLEASE_WAIT_" + minutesLeft + "_MINUTES");
        }

        if ("ACTIVE".equals(user.getStatus())) {
            throw new RuntimeException("PHONE_ALREADY_VERIFIED");
        }

        VerificationOtpCode latestOtp = verificationOtpCodeRepository
                .findTopByUserAndIsUsedFalseOrderByCreatedAtDesc(user)
                .orElseThrow(() -> new RuntimeException("NO_ACTIVE_OTP_FOUND"));

        if (latestOtp.getExpiryDate().isBefore(now) || latestOtp.getAttemptCount() >= 5) {
            latestOtp.setUsed(true);
            verificationOtpCodeRepository.save(latestOtp);
            throw new RuntimeException("OTP_INVALID_OR_EXPIRED");
        }

        if (latestOtp.getOtpCode().equals(otpCode)) {
            user.setStatus("ACTIVE");
            user.setFailedAttemptCount(0);
            user.setLockedUntil(null);
            userRepository.save(user);

            latestOtp.setUsed(true);
            verificationOtpCodeRepository.save(latestOtp);
        } else {
            otpLockoutManager.recordFailedAttempt(latestOtp, user);

            int remaining = 5 - latestOtp.getAttemptCount();
            throw new RuntimeException("INVALID_OTP_REMAINING_" + Math.max(0, remaining));
        }
    }

    @Override
    @Transactional
    public void resendOtp(String phone) {
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("USER_NOT_FOUND"));

        VerificationOtpCode lastEntry = verificationOtpCodeRepository
                .findTopByUserOrderByCreatedAtDesc(user)
                .orElseThrow(() -> new RuntimeException("NO_OTP_HISTORY"));

        Instant now = Instant.now();

        if (lastEntry.getLastResendAt() != null) {
            Instant nextAllowed = lastEntry.getLastResendAt().plus(Duration.ofMinutes(2));
            if (now.isBefore(nextAllowed)) {
                long secondsLeft = Duration.between(now, nextAllowed).getSeconds();
                throw new RuntimeException("COOLDOWN_ACTIVE_" + secondsLeft + "_SECONDS_LEFT");
            }
        }

        if (lastEntry.getResendCount() >= 5) {
            user.setStatus("BANNED");
            user.setLockedUntil(now.plus(Duration.ofHours(24)));
            userRepository.save(user);
            throw new RuntimeException("MAX_RESEND_BANNED_24H");
        }

        VerificationOtpCode latestOtp = verificationOtpCodeRepository
                .findTopByUserOrderByCreatedAtDesc(user)
                .orElse(null);

        if (latestOtp != null && latestOtp.getLastResendAt() != null) {
            Instant nextAllowed = latestOtp.getLastResendAt().plusSeconds(120);
            if (now.isBefore(nextAllowed)) {
                long secondsLeft = Duration.between(now, nextAllowed).getSeconds();
                throw new RuntimeException("PLEASE_WAIT_BEFORE_RESEND_" + secondsLeft + "_SECONDS");
            }
        }

        String newCode = String.format("%06d", new SecureRandom().nextInt(900000) + 100000);
        VerificationOtpCode newOtp = VerificationOtpCode.builder()
                .otpCode(newCode)
                .user(user)
                .expiryDate(now.plus(Duration.ofMinutes(2)))
                .resendCount(lastEntry.getResendCount() + 1)
                .attemptCount(0)
                .lastResendAt(now)
                .isUsed(false)
                .build();

        verificationOtpCodeRepository.save(newOtp);

        log.info("Resend OTP thành công cho phone: {}, lần thứ: {}", phone,
                latestOtp != null ? latestOtp.getResendCount() + 1 : 1);
        eventPublisher.publishEvent(new SmsOtpEvent(phone, newCode));
    }

    /*============================================================================================================*/
    private String generateAndSaveToken(User user) {

        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = VerificationToken.builder()
                .token(token)
                .user(user)
                .expiryDate(Instant.now().plus(Duration.ofHours(24)))
                .build();

        tokenRepository.save(verificationToken);
        log.info("Tạo token cho email: {}", user.getEmail());

        return token;
    }

    private String generateAndSaveOtp(User user) {
//        verificationOtpCodeRepository.deleteByUser(user);
//        verificationOtpCodeRepository.flush();

        SecureRandom secureRandom = new SecureRandom();
        String otpCode = String.format("%06d", secureRandom.nextInt(900000) + 100000);

        VerificationOtpCode verificationOtpCode = VerificationOtpCode.builder()
                .otpCode(otpCode)
                .user(user)
                .expiryDate(Instant.now().plus(Duration.ofSeconds(120)))
                .attemptCount(0)
                .resendCount(0)
                .lastResendAt(Instant.now())
                .isUsed(false)
                .build();

        verificationOtpCodeRepository.save(verificationOtpCode);

        log.info("Tạo OTP cho SDT: {}", user.getPhone());

        return otpCode;

//        telegramSmsService.sendOtp(user.getPhone(), otpCode);
    }

//    private String normalizePhone(String phone) {
//        if (phone == null) return null;
//        String cleaned = phone.replaceAll("[\\s\\-+()]", "");
//        if (cleaned.startsWith("84")) {
//            cleaned = "0" + cleaned.substring(2);
//        }
//        if (!cleaned.matches("^0[1-9]\\d{8}$")) {
//            throw new RuntimeException("INVALID_PHONE_FORMAT");
//        }
//        return cleaned;
//    }
}
