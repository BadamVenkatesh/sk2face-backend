package com.sk2face.authservice.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private String status; // "SUCCESS" or "ERROR"
    private String message; // error message (null on success)
    private T data; // response payload
    private String timestamp;
    private String traceId;

    // ── Factory methods ──────────────────────────────────────────────
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .status("SUCCESS")
                .data(data)
                .timestamp(LocalDateTime.now().toString())
                .traceId(UUID.randomUUID().toString().substring(0, 8))
                .build();
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .status("SUCCESS")
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now().toString())
                .traceId(UUID.randomUUID().toString().substring(0, 8))
                .build();
    }

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .status("ERROR")
                .message(message)
                .timestamp(LocalDateTime.now().toString())
                .traceId(UUID.randomUUID().toString().substring(0, 8))
                .build();
    }
}
