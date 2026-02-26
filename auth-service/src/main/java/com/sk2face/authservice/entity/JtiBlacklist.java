package com.sk2face.authservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "jti_blacklist")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class JtiBlacklist {
    @Id
    private String jti;

    private Instant expiry;
}