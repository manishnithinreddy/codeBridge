package com.codebridge.session.controller;

import com.codebridge.session.dto.KeepAliveResponse;
import com.codebridge.session.dto.SessionResponse;
import com.codebridge.session.dto.SshSessionServiceApiInitRequest; // This DTO is received from ServerService
import com.codebridge.session.exception.AccessDeniedException;
import com.codebridge.session.service.SshSessionLifecycleManager;
import javax.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication; // Added
import org.springframework.web.bind.annotation.*;

import java.util.UUID; // Added

@RestController
@RequestMapping("/api/lifecycle/ssh")
public class SshLifecycleController {

    private static final Logger logger = LoggerFactory.getLogger(SshLifecycleController.class);

    private final SshSessionLifecycleManager sshLifecycleManager;

    public SshLifecycleController(@Qualifier("sshSessionLifecycleManager") SshSessionLifecycleManager sshLifecycleManager) {
        this.sshLifecycleManager = sshLifecycleManager;
    }

    @PostMapping("/init")
    public ResponseEntity<SessionResponse> initSshSession(
            @Valid @RequestBody SshSessionServiceApiInitRequest request,
            Authentication authentication) { // Added Authentication

        UUID tokenPlatformUserId = UUID.fromString(authentication.getName());
        if (!tokenPlatformUserId.equals(request.getPlatformUserId())) {
            logger.warn("Access denied for SSH init: Token platformUserId {} does not match request platformUserId {}.",
                        tokenPlatformUserId, request.getPlatformUserId());
            throw new AccessDeniedException("Authenticated user does not match platformUserId in request.");
        }

        logger.info("Received SSH session init request for user {} and server {}", request.getPlatformUserId(), request.getServerId());
        SessionResponse sessionResponse = sshLifecycleManager.initSshSession(
                request.getPlatformUserId(), // Validated platformUserId
                request.getServerId(),
                request.getConnectionDetails()
        );
        return new ResponseEntity<>(sessionResponse, HttpStatus.CREATED);
    }

    @PostMapping("/{sessionToken}/keepalive")
    public ResponseEntity<KeepAliveResponse> keepaliveSshSession(@PathVariable String sessionToken) {
        // This endpoint is authenticated by the sessionToken itself (SessionService JWT)
        // via logic in SshOperationController's getValidatedLocalSshSession.
        // The User JWT from Authorization header is not strictly needed here for core logic,
        // but Spring Security will validate it if it's present due to the filter chain.
        logger.info("Received SSH session keepalive request for token: {}", sessionToken);
        KeepAliveResponse keepAliveResponse = sshLifecycleManager.keepAliveSshSession(sessionToken);
        return ResponseEntity.ok(keepAliveResponse);
    }

    @PostMapping("/{sessionToken}/release")
    public ResponseEntity<Void> releaseSshSession(@PathVariable String sessionToken) {
        // Similar to keepalive, primary auth is the sessionToken.
        logger.info("Received SSH session release request for token: {}", sessionToken);
        sshLifecycleManager.releaseSshSession(sessionToken);
        return ResponseEntity.noContent().build();
    }
}
