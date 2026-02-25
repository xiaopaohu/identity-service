package com.datn.identityservice.constant;

import com.datn.identityservice.exception.AppException;
import com.datn.identityservice.exception.ErrorCode;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SecurityUtils {

    public static UUID getCurrentUserId() {
        var context = SecurityContextHolder.getContext();
        var authentication = context.getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        Object principal = authentication.getPrincipal();
        String userIdStr;

        if (principal instanceof Jwt jwt) {
            userIdStr = jwt.getSubject();
        } else {
            userIdStr = authentication.getName();
        }
        try {
            return UUID.fromString(userIdStr);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
    }
}
