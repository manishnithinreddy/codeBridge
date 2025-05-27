package com.codebridge.usermanagement.session.controller;

import com.codebridge.usermanagement.session.model.UserSession;
import com.codebridge.usermanagement.session.service.UserSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for user session operations.
 */
@RestController
@RequestMapping("/api/sessions")
public class UserSessionController {

    private static final Logger logger = LoggerFactory.getLogger(UserSessionController.class);

    private final UserSessionService sessionService;

    @Autowired
    public UserSessionController(UserSessionService sessionService) {
        this.sessionService = sessionService;
    }

    /**
     * Get all active sessions for a user.
     *
     * @param userId The user ID
     * @return The sessions
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or @securityService.isCurrentUser(#userId)")
    public ResponseEntity<List<UserSession>> getUserSessions(@PathVariable UUID userId) {
        List<UserSession> sessions = sessionService.findActiveSessionsByUserId(userId);
        return ResponseEntity.ok(sessions);
    }

    /**
     * Deactivate a session.
     *
     * @param id The session ID
     * @return The response
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @securityService.isSessionOwner(#id)")
    public ResponseEntity<?> deactivateSession(@PathVariable UUID id) {
        sessionService.deactivateSession(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Deactivate all sessions for a user.
     *
     * @param userId The user ID
     * @return The response
     */
    @DeleteMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or @securityService.isCurrentUser(#userId)")
    public ResponseEntity<?> deactivateAllUserSessions(@PathVariable UUID userId) {
        int count = sessionService.deactivateAllUserSessions(userId);
        return ResponseEntity.ok().body(count);
    }
}

