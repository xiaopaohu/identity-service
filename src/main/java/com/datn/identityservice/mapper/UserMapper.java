package com.datn.identityservice.mapper;


import com.datn.identityservice.dto.request.EmailRegisterRequest;
import com.datn.identityservice.entity.User;
import com.datn.identityservice.entity.UserRole;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;


@Mapper(componentModel = "spring", imports = {UUID.class, Collections.class, Collectors.class})
public interface UserMapper {

    // --- MAPPING REQUEST -> ENTITY ---

    // 1. Luồng User tự đăng ký (Email/Phone)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true) // Sẽ hash tại Service
    @Mapping(target = "status", constant = "ACTIVE") // Hoặc PENDING tùy logic verify
    @Mapping(target = "userRoles", ignore = true)
    User toUser(EmailRegisterRequest request);

//    @Mapping(target = "id", ignore = true)
//    @Mapping(target = "passwordHash", ignore = true)
//    @Mapping(target = "status", constant = "ACTIVE")
//    @Mapping(target = "userRoles", ignore = true)
//    User toUser(PhoneRegisterRequest request);

//    // 2. Luồng Admin tạo Account (Có gán Role)
//    @Mapping(target = "id", ignore = true)
//    @Mapping(target = "passwordHash", ignore = true)
//    @Mapping(target = "userRoles", ignore = true)
//    // Cần xử lý riêng tại Service để map Role từ DB
//    User toUser(AdminCreateAccountRequest request);


    // --- MAPPING ENTITY -> RESPONSE ---


//    // 3. Trả về thông tin User chuẩn (Bao gồm phẳng hóa Roles)
//    @Mapping(target = "roles", expression = "java(mapUserRolesToNames(user.getUserRoles()))")
//    UserResponse toUserResponse(User user);

//    // 4. Trả về Audit Log
//    AuditLogResponse toAuditLogResponse(AuditLog auditLog);


    // --- HELPER METHODS (Logic xử lý phụ) ---

    // Chuyển từ Set<UserRole> (Entity) sang Set<String> (DTO)
    default Set<String> mapUserRolesToNames(Set<UserRole> userRoles) {
        if (userRoles == null || userRoles.isEmpty()) {
            return Collections.emptySet();
        }
        return userRoles.stream()
                .map(ur -> ur.getRole().getName())
                .collect(Collectors.toSet());
    }
}
