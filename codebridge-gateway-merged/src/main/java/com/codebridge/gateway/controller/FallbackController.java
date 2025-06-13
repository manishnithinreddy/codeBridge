package com.codebridge.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/auth")
    public Mono<ResponseEntity<Map<String, Object>>> authServiceFallback() {
        return createFallbackResponse("Authentication Service is currently unavailable");
    }

    @GetMapping("/user")
    public Mono<ResponseEntity<Map<String, Object>>> userServiceFallback() {
        return createFallbackResponse("User Service is currently unavailable");
    }

    @GetMapping("/server")
    public Mono<ResponseEntity<Map<String, Object>>> serverServiceFallback() {
        return createFallbackResponse("Server Service is currently unavailable");
    }

    @GetMapping("/session")
    public Mono<ResponseEntity<Map<String, Object>>> sessionServiceFallback() {
        return createFallbackResponse("Session Service is currently unavailable");
    }

    @GetMapping("/ai-db-agent")
    public Mono<ResponseEntity<Map<String, Object>>> aiDbAgentServiceFallback() {
        return createFallbackResponse("AI DB Agent Service is currently unavailable");
    }

    private Mono<ResponseEntity<Map<String, Object>>> createFallbackResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("error", "Service Unavailable");
        response.put("message", message);
        response.put("path", "/fallback");
        
        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(response));
    }
}

