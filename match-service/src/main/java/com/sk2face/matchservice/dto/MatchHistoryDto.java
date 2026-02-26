package com.sk2face.matchservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class MatchHistoryDto {

    private Long id;
    private String inputImageUrl;
    private String match1;
    private String match2;
    private String match3;
    private String status;
    private LocalDateTime createdAt;
}
