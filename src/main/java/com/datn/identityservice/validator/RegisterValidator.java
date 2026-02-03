package com.datn.identityservice.validator;

import com.datn.identityservice.dto.request.EmailRegisterRequest;
import org.springframework.stereotype.Component;

@Component
public class RegisterValidator {
    public void validate(EmailRegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("CONFIRM_PASSWORD_NOT_MATCH");
        }
    }
}
