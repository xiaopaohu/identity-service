package com.datn.identityservice.mapper;


import com.datn.identityservice.dto.request.AdminCreateUserRequest;
import com.datn.identityservice.dto.request.AdminUserUpdateRequest;
import com.datn.identityservice.dto.request.EmailRegisterRequest;
import com.datn.identityservice.dto.request.PhoneRegisterRequest;
import com.datn.identityservice.dto.response.UserResponse;
import com.datn.identityservice.entity.User;
import com.datn.identityservice.entity.UserRole;
import org.mapstruct.*;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;


@Mapper(componentModel = "spring", imports = {UUID.class, Collections.class, Collectors.class})
public interface UserMapper {

    // 1. Luồng User tự đăng ký (Email/Phone)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "userRoles", ignore = true)
    User toUser(EmailRegisterRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "userRoles", ignore = true)
    User toUser(PhoneRegisterRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "userRoles", ignore = true)
    User toUser(AdminCreateUserRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "userRoles", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    void updateUser(@MappingTarget User user, AdminUserUpdateRequest request);

    @Mapping(target = "roles", expression = "java(user.getUserRoles().stream().map(ur -> ur.getRole().getName()).collect(java.util.stream.Collectors.toSet()))")
    UserResponse toUserResponse(User user);

    default Set<String> mapUserRolesToNames(Set<UserRole> userRoles) {
        if (userRoles == null || userRoles.isEmpty()) return null;
        return userRoles.stream()
                .map(ur -> ur.getRole().getName())
                .collect(Collectors.toSet());
    }

//    // 4. Trả về Audit Log
//    AuditLogResponse toAuditLogResponse(AuditLog auditLog);

}
