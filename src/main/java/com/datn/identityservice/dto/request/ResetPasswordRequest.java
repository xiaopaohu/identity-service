package com.datn.identityservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ResetPasswordRequest {
    @NotBlank(message = "IDENTIFIER_CANNOT_BE_EMPTY")
    String loginIdentifier;

    @NotBlank(message = "OTP_CANNOT_BE_EMPTY")
    String otp;

    @NotBlank(message = "PASSWORD_INVALID")
    @Size(min = 8, message = "PASSWORD_INVALID")
    String newPassword;

    @NotBlank(message = "CONFIRM_PASSWORD_CANNOT_BE_EMPTY")
    String confirmPassword;
}
