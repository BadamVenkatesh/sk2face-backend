package com.sk2face.authservice.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @Column(length = 36, updatable = false, nullable = false)
    private String uuid;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    @JsonIgnore
    private String password;

    @Column(nullable = false)
    @Builder.Default
    private String roles = "USER";

    @Builder.Default
    private boolean enabled = true;

    @Builder.Default
    private Instant createdAt = Instant.now();

    @PrePersist
    public void prePersist() {
        if (this.uuid == null) {
            this.uuid = java.util.UUID.randomUUID().toString();
        }
    }
}