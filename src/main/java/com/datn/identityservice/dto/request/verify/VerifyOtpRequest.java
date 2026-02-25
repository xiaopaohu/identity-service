package com.datn.identityservice.dto.request.verify;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyOtpRequest {
    @NotBlank(message = "IDENTIFIER_CANNOT_BE_EMPTY")
    String identifier;

    @NotBlank(message = "OTP_CANNOT_BE_EMPTY")
    String otpCode;
}
