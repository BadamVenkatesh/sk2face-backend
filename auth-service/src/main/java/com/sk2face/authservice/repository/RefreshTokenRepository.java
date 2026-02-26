package com.sk2face.authservice.repository;

import com.sk2face.authservice.entity.RefreshToken;
import com.sk2face.authservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    void deleteAllByUser(User user);
}