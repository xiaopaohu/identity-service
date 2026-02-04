package com.datn.identityservice.service.Impl;

import com.datn.identityservice.constant.SecurityUtils;
import com.datn.identityservice.dto.request.AdminCreateUserRequest;
import com.datn.identityservice.dto.request.PasswordUpdateRequest;
import com.datn.identityservice.dto.response.UserResponse;
import com.datn.identityservice.entity.Role;
import com.datn.identityservice.entity.User;
import com.datn.identityservice.mapper.UserMapper;
import com.datn.identityservice.repository.RoleRepository;
import com.datn.identityservice.repository.UserRepository;
import com.datn.identityservice.service.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserServiceImpl implements UserService {
    UserRepository userRepository;
    RoleRepository roleRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;

    @Override
    public UserResponse createUser(AdminCreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail()))
            throw new RuntimeException("EMAIL_ALREADY_EXISTS");

        if (userRepository.existsByPhone(request.getPhone()))
            throw new RuntimeException("PHONE_ALREADY_EXISTS");

        User user = userMapper.toUser(request);

        if (user.getId() == null) {
            user.setId(UUID.randomUUID());
        }

        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setStatus("ACTIVE");
        user.setFailedAttemptCount(0);

        if (!CollectionUtils.isEmpty(request.getRoles())) {
            request.getRoles().forEach(roleName -> {
                String formattedRoleName = roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName;

                Role role = roleRepository.findByName(formattedRoleName)
                        .orElseThrow(() -> new RuntimeException("ROLE_NOT_FOUND: " + formattedRoleName));
                user.addRole(role);
            });
        }

        return userMapper.toUserResponse(userRepository.save(user));
    }

    @Override
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toUserResponse)
                .collect(Collectors.toList());
    }

    @Override
    public UserResponse getUser(UUID id) {
        return userRepository.findById(id)
                .map(userMapper::toUserResponse)
                .orElseThrow(() -> new RuntimeException("USER_NOT_FOUND"));
    }

    @Override
    public void updatePassword(PasswordUpdateRequest request) {
        String currentId = SecurityUtils.getCurrentUserId();

        User user = userRepository.findById(UUID.fromString(currentId))
                .orElseThrow(() -> new RuntimeException("USER_NOT_FOUND"));
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new RuntimeException("OLD_PASSWORD_INCORRECT");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(Instant.now());

        userRepository.save(user);
    }
}
