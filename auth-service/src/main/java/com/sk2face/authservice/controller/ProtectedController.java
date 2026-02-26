package com.sk2face.authservice.controller;

import com.sk2face.authservice.common.ApiResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ProtectedController {

    @GetMapping("/profile")
    public ApiResponse<Map<String, String>> profile(Authentication auth) {
        return ApiResponse.success(Map.of("username", auth.getName()));
    }
}