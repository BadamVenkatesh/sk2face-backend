package com.sk2face.matchservice.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "match_requests")
@Data
public class MatchRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String inputImageUrl;
    private String matchResult1;
    private String matchResult2;
    private String matchResult3;
    private String status;
    private LocalDateTime createdAt;
}
