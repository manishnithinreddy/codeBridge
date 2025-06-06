package com.codebridge.server.controller;

import com.codebridge.server.dto.ServerActivityLogResponse;
import com.codebridge.server.service.ServerActivityLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
// Import AccessDeniedException if specific check is needed beyond Spring Security's default
// import com.codebridge.server.exception.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/activity-logs")
public class ServerActivityLogController {

    private final ServerActivityLogService serverActivityLogService;

    public ServerActivityLogController(ServerActivityLogService serverActivityLogService) {
        this.serverActivityLogService = serverActivityLogService;
    }

    private UUID getPlatformUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("Authentication principal not found or username is null.");
        }
        return UUID.fromString(authentication.getName());
    }

    @GetMapping("/server/{serverId}")
    public ResponseEntity<Page<ServerActivityLogResponse>> getLogsForServer(
            @PathVariable UUID serverId,
            @PageableDefault(size = 20, sort = "timestamp") Pageable pageable,
            Authentication authentication) {
        // Optional: Add authorization check if only specific users (e.g., server owner, admin) can view server logs
        // For now, assuming any authenticated user with access to this endpoint can query by serverId.
        // A more robust check might involve ensuring user has some relation to the serverId.
        Page<ServerActivityLogResponse> logs = serverActivityLogService.getLogsForServer(serverId, pageable);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/user/{logUserId}")
    public ResponseEntity<Page<ServerActivityLogResponse>> getLogsForUser(
            @PathVariable UUID logUserId,
            @PageableDefault(size = 20, sort = "timestamp") Pageable pageable,
            Authentication authentication) {
        UUID authenticatedUserId = getPlatformUserId(authentication);

        // Authorization: Allow user to see their own logs.
        // Admins might have broader access (not implemented here, would require role check).
        if (!authenticatedUserId.equals(logUserId)) {
            // Consider throwing Spring's AccessDeniedException or a custom one
            // For now, returning forbidden, but GlobalExceptionHandler might handle it more gracefully
             return ResponseEntity.status(403).build();
            // throw new com.codebridge.server.exception.AccessDeniedException("You are not authorized to view logs for user " + logUserId);
        }

        Page<ServerActivityLogResponse> logs = serverActivityLogService.getLogsForUser(logUserId, pageable);
        return ResponseEntity.ok(logs);
    }
}
