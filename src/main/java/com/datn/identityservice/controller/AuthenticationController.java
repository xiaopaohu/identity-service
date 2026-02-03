package com.datn.identityservice.controller;


import com.datn.identityservice.dto.request.EmailRegisterRequest;
import com.datn.identityservice.dto.response.ApiResponse;
import com.datn.identityservice.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationController {
    AuthenticationService authenticationService;

    @PostMapping("/register")
    public ApiResponse<String> registerByEmail(@RequestBody @Valid EmailRegisterRequest request) {
        authenticationService.registerByEmail(request);
        return ApiResponse.<String>builder()
                .result("Hãy kiểm tra email để xác thực tài khoản cảu bạn!")
                .build();
    }

    //    @PostMapping("/verify"
    @GetMapping("/verify")
    public ApiResponse<String> verifyRegisterByEmail(@RequestParam("token") String token) {
        authenticationService.verifyEmail(token);
        return ApiResponse.<String>builder()
                .result("Tài khoản đã được kích hoạt thành công!")
                .build();
    }
}

