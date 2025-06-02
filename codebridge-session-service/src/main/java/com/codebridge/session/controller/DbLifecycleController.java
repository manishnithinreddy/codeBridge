package com.codebridge.session.controller;

import com.codebridge.session.dto.DbSessionInitRequest;
import com.codebridge.session.dto.KeepAliveResponse;
import com.codebridge.session.dto.SessionResponse;
import com.codebridge.session.service.DbSessionLifecycleManager;
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
@RequestMapping("/api/lifecycle/db") // Differentiated from SSH lifecycle
public class DbLifecycleController {

    private static final Logger log = LoggerFactory.getLogger(DbLifecycleController.class);

    private final DbSessionLifecycleManager dbSessionLifecycleManager;

    public DbLifecycleController(@Qualifier("dbSessionManager") DbSessionLifecycleManager dbSessionLifecycleManager) {
        this.dbSessionLifecycleManager = dbSessionLifecycleManager;
    }

    // TODO: Replace with actual user ID from Spring Security context (e.g., from JWT authentication of the calling service)
    private UUID getCurrentPlatformUserId() {
        // This placeholder is problematic for a microservice.
        // The calling service (e.g., codebridge-server-service) should authenticate itself
        // and then securely pass the intended platformUserId as part of the request.
        // For now, SshSessionServiceApiInitRequest and DbSessionInitRequest expect platformUserId.
        // This controller would extract it from an authenticated principal or a trusted header/request body field.
        // For initDbSession, platformUserId is part of DbSessionInitRequest's caller context, not directly in this service.
        // The methods in DbSessionLifecycleManager take platformUserId as a parameter.
        // The controller's job is to get this from the request (passed by codebridge-server-service).

        // For the purpose of this controller, initDbSession will receive platformUserId in request body.
        // This method is not directly used by initDbSession.
        // It might be used if other lifecycle methods needed implicit user context not in token.
        log.warn("Using placeholder platformUserId in DbLifecycleController. This needs to be replaced with actual authenticated principal's ID or passed in request.");
        return UUID.fromString("00000000-0000-0000-0000-000000000001"); // Placeholder, review usage
    }

    @PostMapping("/init")
    public ResponseEntity<SessionResponse> initializeDbSession(@Valid @RequestBody DbSessionInitRequest request) {
        // The platformUserId should ideally come from an authenticated principal representing the user
        // on whose behalf codebridge-server-service is making this call.
        // Or, codebridge-server-service includes it in a more comprehensive request DTO to this service.
        // For now, the DbSessionInitRequest doesn't have platformUserId directly,
        // but DbSessionLifecycleManager.initDbSession expects it.
        // This implies DbSessionInitRequest should be nested or include platformUserId.

        // Assuming platformUserId is passed by the client (e.g. codebridge-server-service) as part of a larger context
        // For this specific endpoint, we'll assume the calling service (e.g. server-service)
        // will pass platformUserId. The initDbSession method in manager needs it.
        // Let's assume a placeholder or that it's part of a yet-to-be-defined outer request DTO from server-service.
        // For now, using the placeholder to make it runnable.
        // **Correction**: The `DbSessionInitRequest` defined for `codebridge-server-service` *would* gather this.
        // The `SessionService`'s `initDbSession` endpoint should expect `platformUserId` in its request body.
        // The `DbSessionInitRequest` defined in Phase 4 Step 1 for `DbSessionController` in `codebridge-server-service`
        // *did not* include `platformUserId`. This is an inconsistency.
        // `DbSessionLifecycleManager.initDbSession` *does* take `platformUserId`.
        // The DTO for this controller `DbSessionInitRequest` (from previous phase) should be enhanced or replaced.

        // For now, I'll use the placeholder. This highlights a DTO design detail to sync.
        // The SshSessionServiceApiInitRequest included platformUserId. We need similar for DB.
        // Let's assume the DbSessionInitRequest will be enhanced or replaced by one that includes platformUserId.
        // For now, to make progress, I will use the placeholder.

        // UUID platformUserId = getCurrentPlatformUserId(); // THIS IS A STAND-IN. // No longer needed
        UUID platformUserId = request.getPlatformUserId(); // Now obtained from request DTO
        // In a real scenario, platformUserId would be part of the DbSessionInitRequest or from authenticated principal.
        // For this step, we'll proceed with the placeholder and assume the request DTO will be adapted.

        log.info("Received request to initialize DB session for alias: {} by user: {}",
                 request.getDbConnectionAlias(), platformUserId);

        // If DbSessionInitRequest is what SessionService receives, it needs platformUserId.
        // Let's assume it's added to DbSessionInitRequest for now.
        // (If I could modify DbSessionInitRequest now, I would add platformUserId to it)

        SessionResponse sessionResponse = dbSessionLifecycleManager.initDbSession(
            platformUserId,
            request.getDbConnectionAlias(),
            request.getCredentials()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(sessionResponse);
    }

    @PostMapping("/{sessionToken}/keepalive")
    public ResponseEntity<KeepAliveResponse> keepAliveDbSession(@PathVariable String sessionToken) {
        log.info("Received request to keep alive DB session with token: {}", sessionToken);
        Optional<KeepAliveResponse> keepAliveResponseOpt = dbSessionLifecycleManager.keepAliveDbSession(sessionToken);
        if (keepAliveResponseOpt.isPresent()) {
            return ResponseEntity.ok(keepAliveResponseOpt.get());
        } else {
            log.warn("Keepalive failed for DB session token: {}. Session not found, expired, or invalid.", sessionToken);
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{sessionToken}/release")
    public ResponseEntity<Void> releaseDbSession(@PathVariable String sessionToken) {
        log.info("Received request to release DB session with token: {}", sessionToken);
        dbSessionLifecycleManager.releaseDbSession(sessionToken);
        return ResponseEntity.noContent().build();
    }
}
