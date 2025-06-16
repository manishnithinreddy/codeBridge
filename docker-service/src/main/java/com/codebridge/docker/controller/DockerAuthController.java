package com.codebridge.docker.controller;

import com.codebridge.docker.model.AuthRequest;
import com.codebridge.docker.model.AuthResponse;
import com.codebridge.docker.service.DockerAuthService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/auth")
public class DockerAuthController {

    private final DockerAuthService dockerAuthService;

    public DockerAuthController(DockerAuthService dockerAuthService) {
        this.dockerAuthService = dockerAuthService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest authRequest) {
        log.info("Login request received for user: {}", authRequest.getUsername());
        AuthResponse response = dockerAuthService.authenticate(authRequest);
        return ResponseEntity.ok(response);
    }
}

