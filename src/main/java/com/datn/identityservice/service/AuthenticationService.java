package com.datn.identityservice.service;

import com.datn.identityservice.dto.request.*;
import com.datn.identityservice.dto.request.verify.VerifyOtpRequest;
import com.datn.identityservice.dto.response.AuthenticationResponse;
import com.datn.identityservice.dto.response.IntrospectResponse;
import com.datn.identityservice.dto.response.UserResponse;
import com.datn.identityservice.enums.OtpType;
import com.nimbusds.jose.JOSEException;

import java.text.ParseException;

public interface AuthenticationService {
    void registerByEmail(EmailRegisterRequest request);

    void verifyEmail(String token);

    void registerByPhone(PhoneRegisterRequest request);

    void verifyOtp(String identifier, String otpCode, OtpType type);

    void resendOtp(String identifier, OtpType type);

    AuthenticationResponse authenticate(AuthenticationRequest request);

    AuthenticationResponse refreshToken(String requestRefreshToken);

    IntrospectResponse introspect(IntrospectRequest request);

    void logout(LogoutRequest request) throws ParseException, JOSEException;

    UserResponse findUserForReset(String identifier);

    void sendOtpForReset(SendOtpRequest request);

    void verifyOtpForReset(VerifyOtpRequest request);

    void resetPassword(ResetPasswordRequest request);

//    void cleanupUnverifiedUsers();
}
