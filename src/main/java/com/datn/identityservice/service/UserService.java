package com.datn.identityservice.service;

import com.datn.identityservice.dto.request.AdminCreateUserRequest;
import com.datn.identityservice.dto.request.AdminUserUpdateRequest;
import com.datn.identityservice.dto.request.PasswordUpdateRequest;
import com.datn.identityservice.dto.response.AdminUserResponse;
import com.datn.identityservice.dto.response.UserResponse;

import java.util.List;
import java.util.UUID;

public interface UserService {
    UserResponse createUser(AdminCreateUserRequest request);

    List<UserResponse> getAllUsers();

    UserResponse getUser(UUID id);

    void updatePassword(PasswordUpdateRequest request);

    // Admin cập nhật thông tin User
    UserResponse updateUser(UUID userId, AdminUserUpdateRequest request);

    void deleteUser(UUID id);

    AdminUserResponse getAdminUserDetail(UUID id);
}
