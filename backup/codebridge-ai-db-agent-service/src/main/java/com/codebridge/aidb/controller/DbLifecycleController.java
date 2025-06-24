package com.codebridge.aidb.controller;

import com.codebridge.aidb.dto.DbSessionInitRequest;
import com.codebridge.aidb.dto.KeepAliveResponse;
import com.codebridge.aidb.dto.SessionResponse;
import com.codebridge.aidb.service.DbSessionLifecycleManager;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/lifecycle/db")
public class DbLifecycleController {

    private static final Logger logger = LoggerFactory.getLogger(DbLifecycleController.class);

    private final DbSessionLifecycleManager dbLifecycleManager;

    public DbLifecycleController(@Qualifier("dbSessionLifecycleManager") DbSessionLifecycleManager dbLifecycleManager) {
        this.dbLifecycleManager = dbLifecycleManager;
    }

    @PostMapping("/init")
    public ResponseEntity<SessionResponse> initDbSession(
            @Valid @RequestBody DbSessionInitRequest request,
            Authentication authentication) {

        UUID tokenPlatformUserId = UUID.fromString(authentication.getName());
        if (!tokenPlatformUserId.equals(request.getPlatformUserId())) {
            logger.warn("Access denied for DB init: Token platformUserId {} does not match request platformUserId {}.",
                        tokenPlatformUserId, request.getPlatformUserId());
            throw new AccessDeniedException("Authenticated user does not match platformUserId in request.");
        }

        logger.info("Received DB session init request for user {} and alias {}", request.getPlatformUserId(), request.getDbConnectionAlias());
        SessionResponse sessionResponse = dbLifecycleManager.initDbSession(
                request.getPlatformUserId(), // Validated platformUserId
                request.getDbConnectionAlias(),
                request.getCredentials()
        );
        return new ResponseEntity<>(sessionResponse, HttpStatus.CREATED);
    }

    @PostMapping("/{sessionToken}/keepalive")
    public ResponseEntity<KeepAliveResponse> keepAliveDbSession(@PathVariable String sessionToken) {
        logger.info("Received DB session keepalive request for token: {}", sessionToken);
        KeepAliveResponse keepAliveResponse = dbLifecycleManager.keepAliveDbSession(sessionToken);
        return ResponseEntity.ok(keepAliveResponse);
    }

    @PostMapping("/{sessionToken}/release")
    public ResponseEntity<Void> releaseDbSession(@PathVariable String sessionToken) {
        logger.info("Received DB session release request for token: {}", sessionToken);
        dbLifecycleManager.releaseDbSession(sessionToken);
        return ResponseEntity.noContent().build();
    }
}

