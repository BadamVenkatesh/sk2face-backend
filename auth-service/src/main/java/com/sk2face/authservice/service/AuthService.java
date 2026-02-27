package com.sk2face.authservice.service;

import com.sk2face.authservice.client.UserRequest;
import com.sk2face.authservice.client.UserServiceClient;
import com.sk2face.authservice.dto.AuthResponse;
import com.sk2face.authservice.dto.LoginRequest;
import com.sk2face.authservice.dto.RegisterRequest;
import com.sk2face.authservice.entity.JtiBlacklist;
import com.sk2face.authservice.entity.RefreshToken;
import com.sk2face.authservice.entity.User;
import com.sk2face.authservice.repository.JtiBlacklistRepository;
import com.sk2face.authservice.repository.RefreshTokenRepository;
import com.sk2face.authservice.repository.UserRepository;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JtiBlacklistRepository jtiBlacklistRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final UserServiceClient userServiceClient;

    @Value("${jwt.refresh-token-validity-seconds}")
    private long refreshValiditySeconds;

    public AuthService(UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            JtiBlacklistRepository jtiBlacklistRepository,
            JwtService jwtService,
            AuthenticationManager authenticationManager,
            PasswordEncoder passwordEncoder,
            UserServiceClient userServiceClient) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jtiBlacklistRepository = jtiBlacklistRepository;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.userServiceClient = userServiceClient;
    }

    // ── Register ──────────────────────────────────────────────────────
    @Transactional
    public User register(RegisterRequest req) {
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new IllegalArgumentException("Username already registered");
        }

        User user = User.builder()
                .username(req.getUsername().toLowerCase().trim())
                .password(passwordEncoder.encode(req.getPassword()))
                .build();

        user = userRepository.save(user);
        log.info("Registered user: {} (uuid: {})", user.getUsername(), user.getUuid());

        // Call user-service to create the user profile
        try {
            UserRequest userRequest = UserRequest.builder()
                    .userId(user.getUuid())
                    .employeeId(req.getEmployeeId())
                    .fullName(req.getFullName())
                    .officialEmail(req.getOfficialEmail())
                    .designation(req.getDesignation())
                    .departmentName(req.getDepartmentName())
                    .phoneNumber(req.getPhoneNumber())
                    .build();
            userServiceClient.createUser(userRequest);
            log.info("User profile created in user-service for uuid: {}", user.getUuid());
        } catch (Exception e) {
            log.error("Failed to create user profile in user-service for uuid: {}. Error: {}",
                    user.getUuid(), e.getMessage());
            throw new RuntimeException("Registration succeeded but user profile creation failed", e);
        }

        return user;
    }

    // ── Login ─────────────────────────────────────────────────────────
    @Transactional
    public AuthResponse login(LoginRequest req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword()));

        User user = userRepository.findByUsername(req.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return buildAuthResponse(user);
    }

    // ── Logout ────────────────────────────────────────────────────────
    @Transactional
    public void logout(String accessToken) {
        Claims claims = jwtService.parseToken(accessToken).getBody();
        JtiBlacklist entry = JtiBlacklist.builder()
                .jti(claims.getId())
                .expiry(claims.getExpiration().toInstant())
                .build();
        jtiBlacklistRepository.save(entry);
        log.info("Revoked token jti={}", claims.getId());
    }

    // ── Refresh ───────────────────────────────────────────────────────
    @Transactional
    public AuthResponse refresh(String refreshPlain) {
        String hash = sha256Hex(refreshPlain);

        RefreshToken token = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new SecurityException("Invalid refresh token"));

        if (token.isRevoked() || token.getExpiry().isBefore(Instant.now())) {
            throw new SecurityException("Refresh token expired or revoked");
        }

        User user = token.getUser();

        // Rotate: delete old, create new
        refreshTokenRepository.delete(token);

        return buildAuthResponse(user);
    }

    // ── Helpers ───────────────────────────────────────────────────────
    private AuthResponse buildAuthResponse(User user) {
        String jti = jwtService.generateJti();
        var roles = Arrays.asList(user.getRoles().split(","));
        String accessToken = jwtService.generateAccessToken(user.getUsername(), user.getUuid(), roles, jti);
        long expiresAt = Instant.now().plusSeconds(jwtService.getAccessValiditySeconds()).toEpochMilli();

        // Create refresh token
        String refreshPlain = UUID.randomUUID().toString() + "-" + UUID.randomUUID();
        String refreshHash = sha256Hex(refreshPlain);
        Instant expiry = Instant.now().plusSeconds(refreshValiditySeconds);
        RefreshToken rt = RefreshToken.builder()
                .tokenHash(refreshHash)
                .user(user)
                .expiry(expiry)
                .build();
        refreshTokenRepository.save(rt);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .accessTokenExpiresAt(expiresAt)
                .refreshToken(refreshPlain)
                .userUuid(user.getUuid())
                .build();
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : digest)
                sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
