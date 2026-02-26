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

    @PostMapping
    public ResponseEntity<ApiResponse<MatchResponseDto>> match(
            @Valid @RequestBody MatchRequestDto request,
            @RequestHeader("X-USER-ID") String userIdHeader
    ) {
        Long userId = Long.parseLong(userIdHeader);
        MatchResponseDto result =
                service.processMatch(request.getImageUrl(), userId);
        return ResponseEntity.ok(responseBuilder.success(result));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Page<MatchHistoryDto>>> getHistory(
            @RequestHeader("X-USER-ID") String userIdHeader,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        Long userId = Long.parseLong(userIdHeader);

        Page<MatchHistoryDto> history =
                service.getUserMatchHistory(userId, page, size);

        return ResponseEntity.ok(
                responseBuilder.success(history)
        );
    }

}
