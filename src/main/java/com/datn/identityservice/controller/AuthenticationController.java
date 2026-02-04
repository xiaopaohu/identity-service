package com.datn.identityservice.controller;


import com.datn.identityservice.dto.request.*;
import com.datn.identityservice.dto.request.verify.ResendOtpRequest;
import com.datn.identityservice.dto.request.verify.VerifyPhoneRequest;
import com.datn.identityservice.dto.response.ApiResponse;
import com.datn.identityservice.dto.response.AuthenticationResponse;
import com.datn.identityservice.service.AuthenticationService;
import com.nimbusds.jose.JOSEException;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationController {
    AuthenticationService authenticationService;

    @PostMapping("/register-email")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<String> registerByEmail(@RequestBody @Valid EmailRegisterRequest request) {
        authenticationService.registerByEmail(request);
        return ApiResponse.created(null, "Hãy kiểm tra email để xác thực tài khoản của bạn!");
    }

    @GetMapping("/verify-email/{token}")
    public ApiResponse<String> verifyEmail(@PathVariable String token) {
        authenticationService.verifyEmail(token);
        return ApiResponse.success("Tài khoản đã được kích hoạt thành công!");
    }

    @PostMapping("/register-phone")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<String> registerByPhone(@RequestBody @Valid PhoneRegisterRequest request) {
        authenticationService.registerByPhone(request);
        return ApiResponse.created(null, "Hãy kiểm tra tin nhắn SMS để xác thực tài khoản của bạn!");
    }

    @PostMapping("/verify-phone")
    public ApiResponse<String> verifyPhone(@RequestBody @Valid VerifyPhoneRequest request) {
        authenticationService.verifyPhone(request.getPhone(), request.getOtpCode());
        return ApiResponse.success("Số điện thoại đã được xác thực thành công!");
    }

    @PostMapping("/resend-otp")
    public ApiResponse<String> resendOtp(@RequestBody @Valid ResendOtpRequest request) {
        authenticationService.resendOtp(request.getPhone());
        return ApiResponse.success("Mã OTP mới đã được gửi qua SMS!");
    }

    @PostMapping("/login")
    public ApiResponse<AuthenticationResponse> authenticate(@RequestBody @Valid AuthenticationRequest request) {
        return ApiResponse.success(authenticationService.authenticate(request), "Login successfully");
    }

    @PostMapping("/refresh-token")
    public ApiResponse<AuthenticationResponse> refresh(@RequestBody RefreshTokenRequest request) {
        var result = authenticationService.refreshToken(request.getRefreshToken());
        return ApiResponse.success(result, "Token refreshed successfully");
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody LogoutRequest request)
            throws ParseException, JOSEException {
        authenticationService.logout(request);
        return ApiResponse.success(null, "Logout successfully");
    }
}

