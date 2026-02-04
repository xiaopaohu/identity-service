package com.datn.identityservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthenticationRequest {
    @NotBlank(message = "IDENTIFIER_CANNOT_BE_EMPTY")
    String loginIdentifier;

    @NotBlank(message = "PASSWORD_CANNOT_BE_EMPTY")
    String password;
}
