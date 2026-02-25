package com.sk2face.authservice.service;

import io.jsonwebtoken.*;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.*;
import java.security.spec.*;
import java.time.Instant;
import java.util.*;
import java.util.Base64;
import java.util.Date;
import java.util.List;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    @Value("${jwt.access-token-validity-seconds}")
    private long accessValiditySeconds;

    @Value("${jwt.refresh-token-validity-seconds}")
    private long refreshValiditySeconds;

    @Value("${jwt.issuer:auth-service}")
    private String issuer;

    private PrivateKey privateKey;
    private PublicKey publicKey;

    @PostConstruct
    public void initKeys() {
        String privatePem = System.getenv("JWT_PRIVATE_KEY");
        String publicPem = System.getenv("JWT_PUBLIC_KEY");
        if (privatePem == null || publicPem == null) {
            log.warn("JWT_PRIVATE_KEY / JWT_PUBLIC_KEY not set — generating temporary RSA key pair (dev mode only!)");
            try {
                KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
                kpg.initialize(2048);
                KeyPair kp = kpg.generateKeyPair();
                this.privateKey = kp.getPrivate();
                this.publicKey = kp.getPublic();
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("Failed to generate RSA key pair", e);
            }
            return;
        }
        try {
            this.privateKey = loadPrivateKey(privatePem);
            this.publicKey = loadPublicKey(publicPem);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load RSA keys", e);
        }
    }

    private PrivateKey loadPrivateKey(String pem) throws Exception {
        String content = pem.replaceAll("-----\\w+ PRIVATE KEY-----", "").replaceAll("\\s", "");
        byte[] pkcs8 = Base64.getDecoder().decode(content);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(pkcs8);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(spec);
    }

    private PublicKey loadPublicKey(String pem) throws Exception {
        String content = pem.replaceAll("-----\\w+ PUBLIC KEY-----", "").replaceAll("\\s", "");
        byte[] x509 = Base64.getDecoder().decode(content);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(x509);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(spec);
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
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    public Jws<Claims> parseToken(String token) {
        try {
            return Jwts.parserBuilder().setSigningKey(publicKey).build().parseClaimsJws(token);
        } catch (JwtException e) {
            throw e;
        }
    }

    public String extractUsername(String token) {
        return parseToken(token).getBody().getSubject();
    }

    public String generateJti() {
        return UUID.randomUUID().toString();
    }

    public long getAccessValiditySeconds() {
        return accessValiditySeconds;
    }
}