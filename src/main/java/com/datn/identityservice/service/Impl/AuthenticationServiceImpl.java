package com.datn.identityservice.service.Impl;

import com.datn.identityservice.dto.event.EmailOtpEvent;
import com.datn.identityservice.dto.event.RegistrationCompleteEvent;
import com.datn.identityservice.dto.event.SmsOtpEvent;
import com.datn.identityservice.dto.request.*;
import com.datn.identityservice.dto.request.verify.VerifyOtpRequest;
import com.datn.identityservice.dto.response.AuthenticationResponse;
import com.datn.identityservice.dto.response.IntrospectResponse;
import com.datn.identityservice.dto.response.UserResponse;
import com.datn.identityservice.entity.*;
import com.datn.identityservice.enums.OtpType;
import com.datn.identityservice.exception.AppException;
import com.datn.identityservice.exception.ErrorCode;
import com.datn.identityservice.mapper.AuthenticationMapper;
import com.datn.identityservice.mapper.UserMapper;
import com.datn.identityservice.repository.*;
import com.datn.identityservice.service.AuthenticationService;
import com.datn.identityservice.service.JwtProvider;
import com.datn.identityservice.service.OtpLockoutManager;
import com.datn.identityservice.validator.RegisterValidator;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.SignedJWT;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
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
    AuthenticationMapper authenticationMapper;
    PasswordEncoder passwordEncoder;
    JwtProvider jwtProvider;
    InvalidatedTokenRepository invalidatedTokenRepository;
    RefreshTokenRepository refreshTokenRepository;
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

        if (verificationToken.getExpiryAt().isBefore(Instant.now())) {
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

        String otpCode = generateAndSaveOtp(user, request.getPhone(), OtpType.REGISTER);

        log.info("The event for sending an OTP to the phone {} has been triggered!", user.getPhone());
        eventPublisher.publishEvent(new SmsOtpEvent(request.getPhone(), otpCode));
    }

    @Override
    @Transactional
    public void verifyOtp(String identifier, String otpCode, OtpType type) {
//        String normalizedPhone = normalizePhone(phone);

        User user = userRepository.findByEmailOrPhone(identifier)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Instant now = Instant.now();

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
            long minutesLeft = Duration.between(now, user.getLockedUntil()).toMinutes();
            throw new RuntimeException("ACCOUNT_LOCKED_PLEASE_WAIT_" + minutesLeft + "_MINUTES");
        }

        if ("ACTIVE".equals(user.getStatus())) {
            throw new RuntimeException("PHONE_ALREADY_VERIFIED");
        }

        VerificationOtpCode latestOtp = verificationOtpCodeRepository
                .findTopByUserAndTypeAndIsUsedFalseOrderByCreatedAtDesc(user, type)
                .orElseThrow(() -> new AppException(ErrorCode.NO_ACTIVE_OTP_FOUND));

        if (latestOtp.getExpiryAt().isBefore(now) || latestOtp.getAttemptCount() >= 5) {
            latestOtp.setUsed(true);
            verificationOtpCodeRepository.save(latestOtp);
            throw new RuntimeException("OTP_INVALID_OR_EXPIRED");
        }

        if (latestOtp.getOtpCode().equals(otpCode)) {
            if (type == OtpType.REGISTER) {
                user.setStatus("ACTIVE");
                user.setFailedAttemptCount(0);
                user.setLockedUntil(null);
                userRepository.save(user);
            }
            if (type == OtpType.FORGOT_PASSWORD) {

                latestOtp.setUsed(true);
                verificationOtpCodeRepository.save(latestOtp);
            }
        } else {
            otpLockoutManager.recordFailedAttempt(latestOtp, user);

            int remaining = 5 - latestOtp.getAttemptCount();
            throw new RuntimeException("INVALID_OTP_REMAINING_" + Math.max(0, remaining));
        }
    }

    @Override
    @Transactional
    public void resendOtp(String identifier, OtpType type) {
        User user = userRepository.findByEmailOrPhone(identifier)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        VerificationOtpCode lastOtp = verificationOtpCodeRepository
                .findTopByUserAndTypeOrderByCreatedAtDesc(user, type)
                .orElseThrow(() -> new AppException(ErrorCode.NO_OTP_HISTORY));

        Instant now = Instant.now();

        if (lastOtp.getLastResendAt() != null) {
            Instant nextAllowed = lastOtp.getLastResendAt().plus(Duration.ofMinutes(2));
            if (now.isBefore(nextAllowed)) {
                long secondsLeft = Duration.between(now, nextAllowed).getSeconds();
                throw new RuntimeException("COOLDOWN_ACTIVE_" + secondsLeft + "_SECONDS_LEFT");
            }
        }

        if (lastOtp.getResendCount() >= 5) {
            user.setStatus("BANNED");
            user.setLockedUntil(now.plus(Duration.ofHours(24)));
            userRepository.save(user);
            throw new RuntimeException("MAX_RESEND_BANNED_24H");
        }

        String newCode = generateAndSaveOtp(user, identifier, type);

        VerificationOtpCode currentOtp = verificationOtpCodeRepository
                .findTopByUserAndTypeOrderByCreatedAtDesc(user, type).get();
        currentOtp.setResendCount(lastOtp.getResendCount() + 1);
        verificationOtpCodeRepository.save(currentOtp);

        if (identifier.contains("@")) {
            eventPublisher.publishEvent(new EmailOtpEvent(identifier, newCode));
        } else {
            eventPublisher.publishEvent(new SmsOtpEvent(identifier, newCode));
        }
    }

    @Override
    @Transactional
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        User user = userRepository.findByEmailOrPhone(request.getLoginIdentifier())
                .orElseThrow(() -> new RuntimeException("USER_NOT_FOUND"));
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new RuntimeException("ACCOUNT_STATUS_INVALID_" + user.getStatus());
        }

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now())) {
            throw new RuntimeException("ACCOUNT_TEMPORARILY_LOCKED");
        }

        boolean authenticated = passwordEncoder.matches(request.getPassword(), user.getPasswordHash());
        if (!authenticated) {
            int newAttempts = user.getFailedAttemptCount() + 1;
            user.setFailedAttemptCount(newAttempts);

            if (newAttempts >= 5) {
                user.setLockedUntil(Instant.now().plus(30, ChronoUnit.MINUTES));
                userRepository.save(user);
                throw new AppException(ErrorCode.ACCOUNT_TEMPORARILY_LOCKED);
            }

            userRepository.save(user);
            throw new AppException(ErrorCode.INVALID_PASSWORD);
        }

        if (user.getFailedAttemptCount() > 0 || user.getLockedUntil() != null) {
            user.setFailedAttemptCount(0);
            user.setLockedUntil(null);
            userRepository.save(user);
        }

        String accessToken = jwtProvider.generateToken(user, 900);
        String refreshTokenStr = jwtProvider.generateToken(user, jwtProvider.getRefreshTokenExpiry());

        refreshTokenRepository.deleteByUser(user);
        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .user(user)
                .token(refreshTokenStr)
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .build();
        refreshTokenRepository.save(refreshTokenEntity);

        return authenticationMapper.toAuthenticationResponse(user, accessToken, refreshTokenStr, true);
    }

    @Override
    public AuthenticationResponse refreshToken(String requestRefreshToken) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(requestRefreshToken)
                .orElseThrow(() -> new RuntimeException("INVALID_TOKEN"));

        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new RuntimeException("EXPIRED_TOKEN");
        }
        User user = refreshToken.getUser();
        String newAccessToken = jwtProvider.generateToken(user, 900);
        return authenticationMapper.toAuthenticationResponse(user, newAccessToken, requestRefreshToken, true);
    }


    @Override
    public IntrospectResponse introspect(IntrospectRequest request) {
        var token = request.getToken();
        boolean isValid = true;
        String scope = null;
        String subject = null;

        try {
            SignedJWT signedJWT = jwtProvider.verifyToken(token);

            var claimsSet = signedJWT.getJWTClaimsSet();
            String jid = claimsSet.getJWTID();

            if (invalidatedTokenRepository.existsById(jid)) {
                isValid = false;
            }

            if (isValid) {
                scope = claimsSet.getStringClaim("scope");
                subject = claimsSet.getSubject();
            }
        } catch (Exception e) {
            isValid = false;
            log.error("Introspect failed: {}", e.getMessage());
        }

        return IntrospectResponse.builder()
                .valid(isValid)
                .scope(scope)
                .subject(subject)
                .build();
    }

    @Override
    public void logout(LogoutRequest request) throws ParseException, JOSEException {
        try {
            var signedToken = jwtProvider.verifyToken(request.getToken());

            String jid = signedToken.getJWTClaimsSet().getJWTID();
            Date expiryTime = signedToken.getJWTClaimsSet().getExpirationTime();

            InvalidatedToken invalidatedToken = InvalidatedToken.builder()
                    .id(jid)
                    .expiryAt(expiryTime.toInstant())
                    .build();

            invalidatedTokenRepository.save(invalidatedToken);

        } catch (RuntimeException e) {
            log.info("Token already expired, no need to invalidate");
        }
    }

    @Override
    public UserResponse findUserForReset(String identifier) {
        User user = userRepository.findByEmailOrPhone(identifier)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        return UserResponse.builder()
                .email(maskString(user.getEmail()))
                .phone(maskString(user.getPhone()))
                .build();
    }

    @Override
    @Transactional
    public void sendOtpForReset(SendOtpRequest request) {
        User user = userRepository.findByEmailOrPhone(request.getIdentifier())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        String otpCode = generateAndSaveOtp(user, request.getIdentifier(), OtpType.FORGOT_PASSWORD);

        if (request.getIdentifier().contains("@")) {
            eventPublisher.publishEvent(new EmailOtpEvent(user.getEmail(), otpCode));
        } else {
            eventPublisher.publishEvent(new SmsOtpEvent(user.getPhone(), otpCode));
        }
    }

    @Override
    @Transactional
    public void verifyOtpForReset(VerifyOtpRequest request) {
        User user = userRepository.findByEmailOrPhone(request.getIdentifier())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        VerificationOtpCode latestOtp = verificationOtpCodeRepository
                .findTopByUserAndTypeAndIsUsedFalseOrderByCreatedAtDesc(user, OtpType.FORGOT_PASSWORD)
                .orElseThrow(() -> new AppException(ErrorCode.NO_ACTIVE_OTP_FOUND));

        if (latestOtp.getExpiryAt().isBefore(Instant.now())) {
            throw new AppException(ErrorCode.OTP_EXPIRED);
        }

        if (!latestOtp.getOtpCode().equals(request.getOtpCode())) {
            otpLockoutManager.recordFailedAttempt(latestOtp, user);
            int remaining = 5 - latestOtp.getAttemptCount();
            throw new RuntimeException("Mã OTP sai. Bạn còn " + Math.max(0, remaining) + " lần thử.");
        }

        latestOtp.setExpiryAt(Instant.now().plus(Duration.ofMinutes(15)));
        verificationOtpCodeRepository.save(latestOtp);
        log.info("OTP hợp lệ. Đã gia hạn đến: {} để User đổi mật khẩu", latestOtp.getExpiryAt());
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new AppException(ErrorCode.PASSWORD_NOT_MATCHED);
        }

        User user = userRepository.findByEmailOrPhone(request.getLoginIdentifier())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        VerificationOtpCode latestOtp = verificationOtpCodeRepository
                .findTopByUserAndTypeAndIsUsedFalseOrderByCreatedAtDesc(user, OtpType.FORGOT_PASSWORD)
                .orElseThrow(() -> new AppException(ErrorCode.NO_ACTIVE_OTP_FOUND));

        if (!latestOtp.getOtpCode().equals(request.getOtp())) {
            throw new AppException(ErrorCode.INVALID_OTP);
        }

        if (latestOtp.getExpiryAt().isBefore(Instant.now())) {
            throw new AppException(ErrorCode.OTP_EXPIRED);
        }

        // 5. Đổi pass và xóa dấu vết
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setFailedAttemptCount(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        latestOtp.setUsed(true);
        verificationOtpCodeRepository.save(latestOtp);

        log.info("Mật khẩu của User {} đã được đặt lại an toàn.", request.getLoginIdentifier());
    }


    /*============================================================================================================*/
    private String generateAndSaveToken(User user) {

        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = VerificationToken.builder()
                .token(token)
                .user(user)
                .expiryAt(Instant.now().plus(Duration.ofHours(24)))
                .build();

        tokenRepository.save(verificationToken);
        log.info("Tạo token cho email: {}", user.getEmail());

        return token;
    }

    private String generateAndSaveOtp(User user, String identifier, OtpType type) {

        SecureRandom secureRandom = new SecureRandom();
        String otpCode = String.format("%06d", secureRandom.nextInt(900000) + 100000);

        VerificationOtpCode verificationOtpCode = VerificationOtpCode.builder()
                .otpCode(otpCode)
                .user(user)
                .type(type)
                .expiryAt(Instant.now().plus(Duration.ofSeconds(120)))
                .attemptCount(0)
                .resendCount(0)
                .lastResendAt(Instant.now())
                .isUsed(false)
                .build();

        verificationOtpCodeRepository.save(verificationOtpCode);

        log.info("Tạo OTP [{}] cho: {}", type, identifier);

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

    private String maskString(String input) {
        if (input == null || input.isEmpty()) return "";

        if (input.contains("@")) {
            String[] parts = input.split("@");
            String name = parts[0];
            String domain = parts[1];
            if (name.length() <= 2) return input;
            return name.substring(0, 2) + "***@" + domain;
        }

        if (input.length() >= 9) {
            return input.substring(0, 3) + "****" + input.substring(input.length() - 3);
        }

        return "******";
    }
}
