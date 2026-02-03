package com.datn.identityservice.controller;


import com.datn.identityservice.dto.request.EmailRegisterRequest;
import com.datn.identityservice.dto.request.PhoneRegisterRequest;
import com.datn.identityservice.dto.request.verify.ResendOtpRequest;
import com.datn.identityservice.dto.request.verify.VerifyPhoneRequest;
import com.datn.identityservice.dto.response.ApiResponse;
import com.datn.identityservice.service.Impl.AuthenticationServiceImpl;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationController {
    AuthenticationServiceImpl authenticationServiceImpl;

    @PostMapping("/register-email")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<String> registerByEmail(@RequestBody @Valid EmailRegisterRequest request) {
        authenticationServiceImpl.registerByEmail(request);
        return ApiResponse.created(null, "Hãy kiểm tra email để xác thực tài khoản của bạn!");
    }

    @GetMapping("/verify-email/{token}")
    public ApiResponse<String> verifyEmail(@PathVariable String token) {
        authenticationServiceImpl.verifyEmail(token);
        return ApiResponse.success("Tài khoản đã được kích hoạt thành công!");
    }

    @PostMapping("/register-phone")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<String> registerByPhone(@RequestBody @Valid PhoneRegisterRequest request) {
        authenticationServiceImpl.registerByPhone(request);
        return ApiResponse.created(null, "Hãy kiểm tra tin nhắn SMS để xác thực tài khoản của bạn!");
    }

    @PostMapping("/verify-phone")
    public ApiResponse<String> verifyPhone(@RequestBody @Valid VerifyPhoneRequest request) {
        authenticationServiceImpl.verifyPhone(request.getPhone(), request.getOtpCode());
        return ApiResponse.success("Số điện thoại đã được xác thực thành công!");
    }

    @PostMapping("/resend-otp")
    public ApiResponse<String> resendOtp(@RequestBody @Valid ResendOtpRequest request) {
        authenticationServiceImpl.resendOtp(request.getPhone());
        return ApiResponse.success("Mã OTP mới đã được gửi qua SMS!");
    }
}

