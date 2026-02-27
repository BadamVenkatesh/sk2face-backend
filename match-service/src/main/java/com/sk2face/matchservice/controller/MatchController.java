package com.sk2face.matchservice.controller;

import com.sk2face.matchservice.dto.MatchHistoryDto;
import com.sk2face.matchservice.dto.MatchRequestDto;
import com.sk2face.matchservice.dto.MatchResponseDto;
import com.sk2face.matchservice.service.MatchService;
import com.sk2face.matchservice.util.ApiResponse;
import com.sk2face.matchservice.util.ResponseBuilder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/match")
@RequiredArgsConstructor
@Validated
public class MatchController {

    private final MatchService service;
    private final ResponseBuilder responseBuilder;

    @PostMapping(consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<MatchResponseDto>> match(
            @RequestParam("image") org.springframework.web.multipart.MultipartFile image,
            @RequestHeader("X-USER-ID") String userIdHeader
    ) throws java.io.IOException {
        Long userId = Long.parseLong(userIdHeader);

        // We use /app/data because the ML service mounts to /app/data
        String tempDir = "/app/data";
        java.io.File directory = new java.io.File(tempDir);
        if (!directory.exists()) {
             // Fallback for local execution
             tempDir = System.getProperty("java.io.tmpdir");
             directory = new java.io.File(tempDir);
        }

        String filename = java.util.UUID.randomUUID().toString() + "_" + image.getOriginalFilename();
        java.io.File savedFile = new java.io.File(directory, filename);
        image.transferTo(savedFile);

        MatchResponseDto result =
                service.processMatch(savedFile.getAbsolutePath(), userId);
        return ResponseEntity.ok(responseBuilder.success(result));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Page<MatchHistoryDto>>> getHistory(
            @RequestHeader("X-USER-ID") String userIdHeader,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        Page<MatchHistoryDto> history =
                service.getUserMatchHistory(userIdHeader, page, size);

        return ResponseEntity.ok(
                responseBuilder.success(history)
        );
    }

}
