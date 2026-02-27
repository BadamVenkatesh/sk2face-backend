package com.sk2face.authservice.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    @Value("${jwt.access-token-validity-seconds}")
    private long accessValiditySeconds;

    @Value("${jwt.refresh-token-validity-seconds}")
    private long refreshValiditySeconds;

    @Value("${jwt.issuer:auth-service}")
    private String issuer;

    @Value("${jwt.secret}")
    private String secret;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(String subject, String userUuid, List<String> roles, String jti) {
        Instant now = Instant.now();
        Date iat = Date.from(now);
        Date exp = Date.from(now.plusSeconds(accessValiditySeconds));
        return Jwts.builder()
                .setSubject(subject)
                .setIssuer(issuer)
                .setIssuedAt(iat)
                .setExpiration(exp)
                .setId(jti)
                .claim("uuid", userUuid)
                .claim("roles", roles)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Jws<Claims> parseToken(String token) {
        try {
            return Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token);
        } catch (JwtException e) {
            throw e;
        }
    }

    public String extractUsername(String token) {
        return parseToken(token).getBody().getSubject();
    }

    public String extractUserUuid(String token) {
        return parseToken(token).getBody().get("uuid", String.class);
    }

    public String generateJti() {
        return UUID.randomUUID().toString();
    }

    public long getAccessValiditySeconds() {
        return accessValiditySeconds;
    }
}