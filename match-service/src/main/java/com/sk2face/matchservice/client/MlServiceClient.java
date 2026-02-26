package com.sk2face.matchservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient(name = "ml-service", url = "http://ml-service:9090")
public interface MlServiceClient {
    @PostMapping("/match")
    Map<String, List<String>> getMatches(@RequestBody Map<String, String> request);
}
