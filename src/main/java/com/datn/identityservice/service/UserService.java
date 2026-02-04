package com.datn.identityservice.service;

import com.datn.identityservice.dto.request.AdminCreateUserRequest;
import com.datn.identityservice.dto.request.PasswordUpdateRequest;
import com.datn.identityservice.dto.response.UserResponse;

import java.util.List;
import java.util.UUID;

public interface UserService {
    UserResponse createUser(AdminCreateUserRequest request);

    List<UserResponse> getAllUsers();

    UserResponse getUser(UUID id);

    void updatePassword(PasswordUpdateRequest request);

//    UserResponse adminUpdateUser(UUID userId, AdminUserUpdateRequest request);
}
