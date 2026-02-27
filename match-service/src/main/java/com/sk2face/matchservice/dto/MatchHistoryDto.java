package com.sk2face.matchservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class MatchHistoryDto {

    private Long id;
    private String inputImageUrl;
    private MatchResultDto match1;
    private MatchResultDto match2;
    private MatchResultDto match3;
    private String status;
    private LocalDateTime createdAt;
}
