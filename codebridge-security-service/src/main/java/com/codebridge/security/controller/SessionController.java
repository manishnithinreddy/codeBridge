package com.codebridge.security.controller;

import com.codebridge.core.security.SecuredMethod;
import com.codebridge.core.security.UserPrincipal;
import com.codebridge.security.dto.SessionCreateRequest;
import com.codebridge.security.dto.SessionResponse;
import com.codebridge.security.model.UserSession;
import com.codebridge.security.service.SessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Controller for session management operations.
 */
@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    /**
     * Creates a new session.
     *
     * @param request the session create request
     * @param userPrincipal the authenticated user
     * @return the created session
     */
    @PostMapping
    @SecuredMethod(roles = {"ROLE_USER", "ROLE_ADMIN"})
    public Mono<ResponseEntity<SessionResponse>> createSession(
            @RequestBody SessionCreateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        return sessionService.createSession(
                UUID.fromString(userPrincipal.getId()),
                request.getIpAddress(),
                request.getUserAgent(),
                request.getDeviceInfo(),
                request.getGeoLocation(),
                request.getRefreshToken()
        ).map(session -> ResponseEntity.ok(mapToResponse(session)));
    }

    /**
     * Gets all active sessions for the authenticated user.
     *
     * @param userPrincipal the authenticated user
     * @return list of active sessions
     */
    @GetMapping
    @SecuredMethod(roles = {"ROLE_USER", "ROLE_ADMIN"})
    public Flux<SessionResponse> getUserSessions(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return sessionService.getUserSessions(UUID.fromString(userPrincipal.getId()))
                .map(this::mapToResponse);
    }

    /**
     * Gets a session by its token.
     *
     * @param token the session token
     * @return the session if found
     */
    @GetMapping("/{token}")
    @SecuredMethod(roles = {"ROLE_USER", "ROLE_ADMIN"})
    public Mono<ResponseEntity<SessionResponse>> getSessionByToken(@PathVariable String token) {
        return sessionService.getSessionByToken(token)
                .map(session -> ResponseEntity.ok(mapToResponse(session)))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Extends a session's expiry time.
     *
     * @param token the session token
     * @return the updated session
     */
    @PutMapping("/{token}/extend")
    @SecuredMethod(roles = {"ROLE_USER", "ROLE_ADMIN"})
    public Mono<ResponseEntity<SessionResponse>> extendSession(@PathVariable String token) {
        return sessionService.extendSession(token)
                .map(session -> ResponseEntity.ok(mapToResponse(session)))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Deactivates a session.
     *
     * @param token the session token
     * @param userPrincipal the authenticated user
     * @return the response entity
     */
    @DeleteMapping("/{token}")
    @SecuredMethod(roles = {"ROLE_USER", "ROLE_ADMIN"})
    public Mono<ResponseEntity<?>> deactivateSession(
            @PathVariable String token,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        return sessionService.deactivateSession(token, UUID.fromString(userPrincipal.getId()))
                .map(deactivated -> {
                    if (deactivated) {
                        return ResponseEntity.ok().body("Session deactivated successfully");
                    } else {
                        return ResponseEntity.notFound().build();
                    }
                });
    }

    /**
     * Deactivates all sessions for the authenticated user.
     *
     * @param userPrincipal the authenticated user
     * @return the response entity
     */
    @DeleteMapping("/all")
    @SecuredMethod(roles = {"ROLE_USER", "ROLE_ADMIN"})
    public Mono<ResponseEntity<?>> deactivateAllSessions(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return sessionService.deactivateAllUserSessions(UUID.fromString(userPrincipal.getId()))
                .map(count -> ResponseEntity.ok().body("Deactivated " + count + " sessions"));
    }

    /**
     * Maps a user session entity to a session response DTO.
     *
     * @param session the user session entity
     * @return the session response DTO
     */
    private SessionResponse mapToResponse(UserSession session) {
        SessionResponse response = new SessionResponse();
        response.setId(session.getId());
        response.setUserId(session.getUserId());
        response.setSessionToken(session.getSessionToken());
        response.setIpAddress(session.getIpAddress());
        response.setUserAgent(session.getUserAgent());
        response.setDeviceInfo(session.getDeviceInfo());
        response.setGeoLocation(session.getGeoLocation());
        response.setLastActivityAt(session.getLastActivityAt());
        response.setExpiresAt(session.getExpiresAt());
        response.setActive(session.isActive());
        response.setCreatedAt(session.getCreatedAt());
        return response;
    }
}

