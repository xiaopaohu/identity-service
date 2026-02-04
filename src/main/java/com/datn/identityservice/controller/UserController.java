package com.datn.identityservice.controller;

import com.datn.identityservice.dto.request.AdminCreateUserRequest;
import com.datn.identityservice.dto.request.PasswordUpdateRequest;
import com.datn.identityservice.dto.response.ApiResponse;
import com.datn.identityservice.dto.response.UserResponse;
import com.datn.identityservice.service.UserService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserController {
    UserService userService;

    @PostMapping
    public ApiResponse<UserResponse> createUser(@RequestBody @Valid AdminCreateUserRequest request) {
        return ApiResponse.success(userService.createUser(request), "User created successfully");
    }

    @GetMapping
    public ApiResponse<List<UserResponse>> getAllUsers() {
        return ApiResponse.success(userService.getAllUsers(), "Fetched all users");
    }

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getUser(@PathVariable UUID id) {
        return ApiResponse.success(userService.getUser(id), "Fetched user detail");
    }

    @PatchMapping("/my-password")
    public ApiResponse<Void> updateMyPassword(@RequestBody @Valid PasswordUpdateRequest request) {
        userService.updatePassword(request);
        return ApiResponse.success(null, "Password updated successfully");
    }

}
