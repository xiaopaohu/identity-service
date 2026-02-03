package com.datn.identityservice.service;

import com.datn.identityservice.dto.request.EmailRegisterRequest;
import com.datn.identityservice.dto.request.PhoneRegisterRequest;

public interface AuthenticationService {
    void registerByEmail(EmailRegisterRequest request);

    void verifyEmail(String token);

    void registerByPhone(PhoneRegisterRequest request);

    void verifyPhone(String phone, String otpCode);

    void resendOtp(String phone);

//    void cleanupUnverifiedUsers();
}
