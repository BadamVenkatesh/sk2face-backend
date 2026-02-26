package com.sk2face.authservice.controller;

import com.sk2face.authservice.common.ApiResponse;
import com.sk2face.authservice.dto.AuthResponse;
import com.sk2face.authservice.dto.LoginRequest;
import com.sk2face.authservice.dto.RegisterRequest;
import com.sk2face.authservice.entity.User;
import com.sk2face.authservice.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, String>>> register(@Valid @RequestBody RegisterRequest req) {
        User user = authService.register(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registered successfully",
                        Map.of("userUuid", user.getUuid(), "username", user.getUsername())));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest req) {
        AuthResponse resp = authService.login(req);
        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("refreshToken is required"));
        }
        AuthResponse resp = authService.refresh(refreshToken);
        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest req) {
        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Authorization header with Bearer token required"));
        }
        authService.logout(header.substring(7));
        return ResponseEntity.ok(ApiResponse.success("Logged out", null));
    }
}
