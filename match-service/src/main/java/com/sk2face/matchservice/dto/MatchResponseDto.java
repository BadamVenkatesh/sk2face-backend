package com.sk2face.matchservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MatchResponseDto {
    private String match1;
    private String match2;
    private String match3;
}
