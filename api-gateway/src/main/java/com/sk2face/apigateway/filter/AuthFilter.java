package com.sk2face.apigateway.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class AuthFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(AuthFilter.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Paths that do NOT require authentication.
     */
    private static final List<String> OPEN_PATHS = List.of(
            "/auth/register",
            "/auth/login",
            "/auth/refresh",
            "/auth/validate",
            "/actuator",
            "/eureka"
    );

    private final WebClient webClient;

    public AuthFilter(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // Skip authentication for open paths
        if (isOpenPath(path)) {
            return chain.filter(exchange);
        }

        // Extract Authorization header
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return onUnauthorized(exchange, "Missing or invalid Authorization header");
        }

        // Call auth-service to validate the token
        return webClient.get()
                .uri("lb://SK2FACE-AUTH-SERVICE/auth/validate")
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(responseBody -> {
                    try {
                        JsonNode root = mapper.readTree(responseBody);
                        String status = root.path("status").asText();

                        if (!"SUCCESS".equals(status)) {
                            return onUnauthorized(exchange, "Token validation failed");
                        }

                        String userId = root.path("data").path("userId").asText();
                        String username = root.path("data").path("username").asText();

                        if (userId == null || userId.isBlank()) {
                            return onUnauthorized(exchange, "Invalid token payload");
                        }

                        log.info("Authenticated user: {} ({})", username, userId);

                        // Mutate the request to add X-USER-ID header
                        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                                .header("X-USER-ID", userId)
                                .header("X-USERNAME", username)
                                .build();

                        return chain.filter(exchange.mutate().request(mutatedRequest).build());

                    } catch (Exception e) {
                        log.error("Error parsing auth response: {}", e.getMessage());
                        return onUnauthorized(exchange, "Authentication error");
                    }
                })
                .onErrorResume(error -> {
                    log.error("Auth validation call failed: {}", error.getMessage());
                    return onUnauthorized(exchange, "Unauthorized");
                });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private boolean isOpenPath(String path) {
        return OPEN_PATHS.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> onUnauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = String.format(
                "{\"status\":\"ERROR\",\"message\":\"%s\"}", message);
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(body.getBytes()))
        );
    }
}
