package com.codebridge.session.controller;

import com.codebridge.session.dto.KeepAliveResponse;
import com.codebridge.session.dto.SessionResponse;
import com.codebridge.session.dto.SshSessionServiceApiInitRequest; // Defined in previous phase
import com.codebridge.session.service.SshSessionLifecycleManager;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/lifecycle/ssh") // Matching API design
public class SshLifecycleController {

    private static final Logger log = LoggerFactory.getLogger(SshLifecycleController.class);

    private final SshSessionLifecycleManager sshSessionLifecycleManager;

    public SshLifecycleController(SshSessionLifecycleManager sshSessionLifecycleManager) {
        this.sshSessionLifecycleManager = sshSessionLifecycleManager;
    }

    // Removed getCurrentPlatformUserId placeholder

    @PostMapping("/init")
    public ResponseEntity<SessionResponse> initializeSession(
            @Valid @RequestBody SshSessionServiceApiInitRequest request,
            Authentication authentication) {

        String platformUserIdString = authentication.getName(); // Get from "sub" claim
        UUID platformUserId;
        try {
            platformUserId = UUID.fromString(platformUserIdString);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid UUID format for platformUserId from token subject: {}", platformUserIdString);
            // This case should ideally not happen if IdP issues valid UUIDs in 'sub'.
            // Depending on policy, could return 400 or 403.
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        // Verify that the platformUserId in the token matches the one in the request, if present.
        // The SshSessionServiceApiInitRequest DTO now contains platformUserId.
        // This is an important ownership/consistency check.
        if (!platformUserId.equals(request.getPlatformUserId())) {
            log.warn("Mismatch between authenticated user ID ({}) and platformUserId in request body ({}). Denying init.",
                     platformUserId, request.getPlatformUserId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); // Or BAD_REQUEST
        }

        log.info("User {} authenticated. Initializing SSH session for server: {} via SessionService",
                 platformUserId, request.getServerId());

        SessionResponse sessionResponse = sshSessionLifecycleManager.initSshSession(
            request.getPlatformUserId(), // Use ID from request body, now verified against token
            request.getServerId(),
            request.getConnectionDetails()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(sessionResponse);
    }

    @PostMapping("/{sessionToken}/keepalive")
    public ResponseEntity<KeepAliveResponse> keepAliveSession(
            @PathVariable String sessionToken,
            Authentication authentication) { // Authentication principal can be used for logging/auditing if needed

        log.info("User {} authenticated. Keeping alive SSH session with token: {}", authentication.getName(), sessionToken);
        Optional<KeepAliveResponse> keepAliveResponseOpt = sshSessionLifecycleManager.keepAliveSshSession(sessionToken);

        if (keepAliveResponseOpt.isPresent()) {
            return ResponseEntity.ok(keepAliveResponseOpt.get());
        } else {
            log.warn("Keepalive failed for SSH session token: {}. Session not found or expired.", sessionToken);
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{sessionToken}/release")
    public ResponseEntity<Void> releaseSession(
            @PathVariable String sessionToken,
            Authentication authentication) { // For logging/auditing

        log.info("User {} authenticated. Releasing SSH session with token: {}", authentication.getName(), sessionToken);
        sshSessionLifecycleManager.releaseSshSession(sessionToken);
        return ResponseEntity.noContent().build();
    }
}
