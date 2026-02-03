package com.datn.identityservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PhoneRegisterRequest {

    @NotBlank(message = "PHONE_NUMBER_REQUIRED")
//    @Pattern(regexp = "^[0-9]{10}$", message = "INVALID_PHONE_FORMAT")
    String phone;

    @NotBlank(message = "PASSWORD_REQUIRED")
    @Size(min = 8, message = "PASSWORD_TOO_SHORT")
    String password;

    @NotBlank(message = "CONFIRM_PASSWORD_REQUIRED")
    String confirmPassword;
}
