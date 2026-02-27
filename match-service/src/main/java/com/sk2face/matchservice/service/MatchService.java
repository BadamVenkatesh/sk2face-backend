package com.sk2face.matchservice.service;

import com.sk2face.matchservice.client.MlServiceClient;
import com.sk2face.matchservice.dto.MatchHistoryDto;
import com.sk2face.matchservice.dto.MatchResponseDto;
import com.sk2face.matchservice.dto.MatchResultDto;
import com.sk2face.matchservice.entity.MatchRequest;
import com.sk2face.matchservice.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchRepository repository;
    private final MlServiceClient mlServiceClient;

    public MatchResponseDto processMatch(String imageUrl, String userId) {

        Map<String, String> request = Map.of("image_path", imageUrl);

        Map<String, List<MatchResultDto>> response =
                mlServiceClient.getMatches(request);

        List<MatchResultDto> matches = response.get("matches");

        if (matches == null || matches.size() < 3) {
            throw new RuntimeException("Invalid match response from ML service");
        }

        MatchRequest entity = new MatchRequest();
        entity.setUserId(userId);
        entity.setInputImageUrl(imageUrl);
        
        entity.setMatchResult1(matches.get(0).getUrl());
        entity.setMatchScore1(matches.get(0).getScore());
        
        entity.setMatchResult2(matches.get(1).getUrl());
        entity.setMatchScore2(matches.get(1).getScore());
        
        entity.setMatchResult3(matches.get(2).getUrl());
        entity.setMatchScore3(matches.get(2).getScore());
        
        entity.setStatus("COMPLETED");
        entity.setCreatedAt(LocalDateTime.now());

        repository.save(entity);

        return new MatchResponseDto(
                matches.get(0),
                matches.get(1),
                matches.get(2)
        );
    }
    public Page<MatchHistoryDto> getUserMatchHistory(
            String userId,
            int page,
            int size
    ) {

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("createdAt").descending()
        );

        Page<MatchRequest> matchPage =
                repository.findByUserId(userId, pageable);

        return matchPage.map(match -> new MatchHistoryDto(
                match.getId(),
                match.getInputImageUrl(),
                new MatchResultDto(match.getMatchResult1(), match.getMatchScore1()),
                new MatchResultDto(match.getMatchResult2(), match.getMatchScore2()),
                new MatchResultDto(match.getMatchResult3(), match.getMatchScore3()),
                match.getStatus(),
                match.getCreatedAt()
        ));
    }

}
