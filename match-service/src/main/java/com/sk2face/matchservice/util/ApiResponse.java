package com.sk2face.matchservice.util;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ApiResponse<T> {
    private String status;
    private T data;
    private LocalDateTime timeStamp;
    private String traceId;
}
