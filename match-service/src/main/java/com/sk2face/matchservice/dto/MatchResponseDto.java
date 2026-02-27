package com.sk2face.matchservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MatchResponseDto {
    private MatchResultDto match1;
    private MatchResultDto match2;
    private MatchResultDto match3;
}
