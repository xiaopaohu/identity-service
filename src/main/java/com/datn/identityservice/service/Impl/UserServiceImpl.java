package com.datn.identityservice.service.Impl;

import com.datn.identityservice.constant.SecurityUtils;
import com.datn.identityservice.dto.request.AdminCreateUserRequest;
import com.datn.identityservice.dto.request.AdminUserUpdateRequest;
import com.datn.identityservice.dto.request.PasswordUpdateRequest;
import com.datn.identityservice.dto.response.AdminUserResponse;
import com.datn.identityservice.dto.response.UserResponse;
import com.datn.identityservice.entity.Role;
import com.datn.identityservice.entity.User;
import com.datn.identityservice.exception.AppException;
import com.datn.identityservice.exception.ErrorCode;
import com.datn.identityservice.mapper.UserMapper;
import com.datn.identityservice.repository.RoleRepository;
import com.datn.identityservice.repository.UserRepository;
import com.datn.identityservice.service.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

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
    @PreAuthorize("hasRole('ADMIN')")
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
    @Transactional
    public void updatePassword(PasswordUpdateRequest request) {
        UUID currentId = SecurityUtils.getCurrentUserId();

        User user = userRepository.findById(currentId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new RuntimeException("OLD_PASSWORD_INCORRECT");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(Instant.now());

        userRepository.save(user);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse updateUser(UUID userId, AdminUserUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        userMapper.updateUser(user, request);

        if (StringUtils.hasText(request.getPassword())) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            List<Role> roles = roleRepository.findAllByNameIn(request.getRoles());
            if (roles.size() != request.getRoles().size()) {
                throw new AppException(ErrorCode.ROLE_NOT_EXISTED);
            }

            user.getUserRoles().clear();
            roles.forEach(user::addRole);
        }

        return userMapper.toUserResponse(userRepository.save(user));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(UUID id) {
        if (!userRepository.existsById(id))
            throw new AppException(ErrorCode.USER_NOT_EXISTED);
        userRepository.deleteById(id);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public AdminUserResponse getAdminUserDetail(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        return userMapper.toAdminUserResponse(user);
    }
}
