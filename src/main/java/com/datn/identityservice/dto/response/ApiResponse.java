package com.datn.identityservice.dto.response;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    @Builder.Default
    private int code = 3636;

    @Builder.Default
    private String status = "success";

    private String message;

    private T result;

    @Builder.Default
    private Instant timestamp = Instant.now();

    private Object errors;

    public static <T> ApiResponse<T> success(T result, String message) {
        return ApiResponse.<T>builder()
                .code(200)
                .status("success")
                .message(message)
                .result(result)
                .build();
    }

    public static <T> ApiResponse<T> success(String message) {
        return success(null, message);
    }

    public static <T> ApiResponse<T> created(T result, String message) {
        return ApiResponse.<T>builder()
                .code(201)
                .status("success")
                .message(message)
                .result(result)
                .build();
    }

    public static <T> ApiResponse<T> error(int code, String message, Object errors) {
        return ApiResponse.<T>builder()
                .code(code)
                .status("error")
                .message(message)
                .errors(errors)
                .build();
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return error(code, message, null);
    }

}
