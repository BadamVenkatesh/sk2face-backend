package com.sk2face.authservice.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private String accessToken;
    private Long accessTokenExpiresAt;
    private String refreshToken;
    private String userUuid;
}