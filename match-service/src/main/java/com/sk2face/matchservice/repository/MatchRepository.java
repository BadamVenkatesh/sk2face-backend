package com.sk2face.matchservice.repository;

import com.sk2face.matchservice.entity.MatchRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MatchRepository extends JpaRepository<MatchRequest, Long> {
    Page<MatchRequest> findByUserId(String userId, Pageable pageable);
}