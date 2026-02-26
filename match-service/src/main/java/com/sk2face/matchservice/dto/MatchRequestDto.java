package com.sk2face.matchservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MatchRequestDto {
    @NotBlank(message = "Image URL is required")
    @Size(max = 500)
    private String imageUrl;
}
