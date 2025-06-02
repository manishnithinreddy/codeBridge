package com.codebridge.server.controller;

import com.codebridge.server.dto.sessions.DbSessionInitRequest;
import com.codebridge.server.dto.sessions.KeepAliveResponse;
import com.codebridge.server.dto.sessions.SessionResponse;
import com.codebridge.server.sessions.DbSessionWrapper;
import com.codebridge.server.sessions.RedisDbSessionManagerImpl;
// Using RedisDbSessionManagerImpl directly for its specific DB session methods
// import com.codebridge.server.sessions.SessionManager;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/db/sessions")
public class DbSessionController {

    private static final Logger log = LoggerFactory.getLogger(DbSessionController.class);

    private final RedisDbSessionManagerImpl dbSessionManager;

    // Using @Qualifier might not be strictly necessary if injecting the concrete class directly,
    // but kept here if we later refactor to an interface that RedisDbSessionManagerImpl implements
    // and that interface is also implemented by other beans for DbSessionWrapper.
    public DbSessionController(@Qualifier("dbSessionManager") RedisDbSessionManagerImpl dbSessionManager) {
        this.dbSessionManager = dbSessionManager;
    }

    // TODO: Replace with actual user ID from Spring Security context
    private UUID getCurrentPlatformUserId() {
        return UUID.fromString("00000000-0000-0000-0000-000000000000"); // Placeholder
    }

    @PostMapping("/init")
    public ResponseEntity<SessionResponse> initializeDbSession(@Valid @RequestBody DbSessionInitRequest request) {
        UUID platformUserId = getCurrentPlatformUserId();
        log.info("Received request to initialize DB session for alias: {} by user: {}",
                 request.getDbConnectionAlias(), platformUserId);

        SessionResponse sessionResponse = dbSessionManager.initDbSession(
            platformUserId,
            request.getDbConnectionAlias(),
            request.getCredentials()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(sessionResponse);
    }

    @PostMapping("/{sessionToken}/keepalive")
    public ResponseEntity<KeepAliveResponse> keepAliveDbSession(@PathVariable String sessionToken) {
        log.info("Received request to keep alive DB session with token: {}", sessionToken);

        Optional<KeepAliveResponse> keepAliveResponseOpt = dbSessionManager.keepAliveDbSession(sessionToken);

        if (keepAliveResponseOpt.isPresent()) {
            return ResponseEntity.ok(keepAliveResponseOpt.get());
        } else {
            log.warn("Keepalive failed for DB session token: {}. Session not found or expired.", sessionToken);
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{sessionToken}/release")
    public ResponseEntity<Void> releaseDbSession(@PathVariable String sessionToken) {
        log.info("Received request to release DB session with token: {}", sessionToken);
        dbSessionManager.releaseDbSession(sessionToken);
        return ResponseEntity.noContent().build();
    }
}
