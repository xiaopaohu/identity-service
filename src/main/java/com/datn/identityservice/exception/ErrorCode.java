package com.datn.identityservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Uncategorized error", HttpStatus.BAD_REQUEST),
    USER_EXISTED(1002, "User existed", HttpStatus.BAD_REQUEST),
    USERNAME_INVALID(1003, "Username must be at least {min} characters", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(1004, "Password must be at least {min} characters", HttpStatus.BAD_REQUEST),
    USER_NOT_EXISTED(1005, "User not existed", HttpStatus.NOT_FOUND),
    UNAUTHENTICATED(1006, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1007, "You do not have permission", HttpStatus.FORBIDDEN),
    INVALID_DOB(1008, "Your age must be at least {min}", HttpStatus.BAD_REQUEST),
    ROLE_NOT_EXISTED(1009, "Role not existed", HttpStatus.BAD_REQUEST),
    ACCOUNT_TEMPORARILY_LOCKED(1010, "Locked account", HttpStatus.BAD_REQUEST),
    NO_ACTIVE_OTP_FOUND(1011, "No active otp found", HttpStatus.BAD_REQUEST),
    NO_OTP_HISTORY(1012, "No Otp history", HttpStatus.BAD_REQUEST),
    INVALID_OTP(1013, "Invalid Otp", HttpStatus.BAD_REQUEST),
    PASSWORD_NOT_MATCHED(1014, "Password not matched", HttpStatus.BAD_REQUEST),
    OTP_EXPIRED(1015, "Password not matched", HttpStatus.BAD_REQUEST),
    ;

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;
}
