package com.datn.identityservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SendOtpRequest {
    @NotBlank(message = "IDENTIFIER_CANNOT_BE_EMPTY")
    String identifier;

    @NotBlank(message = "METHOD_REQUIRED")
    String method;
}
