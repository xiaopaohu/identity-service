package com.datn.identityservice.service;

import com.datn.identityservice.dto.request.*;
import com.datn.identityservice.dto.response.AuthenticationResponse;
import com.datn.identityservice.dto.response.IntrospectResponse;
import com.nimbusds.jose.JOSEException;

import java.text.ParseException;

public interface AuthenticationService {
    void registerByEmail(EmailRegisterRequest request);

    void verifyEmail(String token);

    void registerByPhone(PhoneRegisterRequest request);

    void verifyPhone(String phone, String otpCode);

    void resendOtp(String phone);

    AuthenticationResponse authenticate(AuthenticationRequest request);

    AuthenticationResponse refreshToken(String requestRefreshToken);

    IntrospectResponse introspect(IntrospectRequest request);

    void logout(LogoutRequest request) throws ParseException, JOSEException;

//    void cleanupUnverifiedUsers();
}
