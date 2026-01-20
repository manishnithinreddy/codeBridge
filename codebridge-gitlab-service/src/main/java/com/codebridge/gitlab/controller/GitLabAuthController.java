package com.codebridge.gitlab.controller;

import com.codebridge.gitlab.model.AuthRequest;
import com.codebridge.gitlab.model.AuthResponse;
import com.codebridge.gitlab.service.GitLabAuthService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for GitLab authentication operations.
 */
@Slf4j
@RestController
@RequestMapping("/auth")
public class GitLabAuthController {

    private static final Logger log = LoggerFactory.getLogger(GitLabAuthController.class);
    
    private final GitLabAuthService gitLabAuthService;

    public GitLabAuthController(GitLabAuthService gitLabAuthService) {
        this.gitLabAuthService = gitLabAuthService;
    }

    /**
     * Authenticates a user with GitLab using personal access token.
     *
     * @param authRequest The authentication request containing username and personal access token
     * @return ResponseEntity containing JWT token for authenticated user
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest authRequest) {
        log.info("Login request received for user: {}", authRequest.getUsername());
        AuthResponse response = gitLabAuthService.authenticate(authRequest);
        return ResponseEntity.ok(response);
    }
}
