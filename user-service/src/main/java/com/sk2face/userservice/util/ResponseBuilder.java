package com.sk2face.userservice.util;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ResponseBuilder {

    public <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .status("SUCCESS")
                .data(data)
                .timeStamp(LocalDateTime.now())
                .traceId(MDC.get("traceId"))
                .build();
    }
}
