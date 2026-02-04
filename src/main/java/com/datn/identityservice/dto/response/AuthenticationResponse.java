package com.datn.identityservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthenticationResponse {
    String accessToken;
    String refreshToken;
    boolean authenticated;

    @Builder.Default
    boolean mfaRequired = false;
    String userId;
    String email;
    String phone;
}
