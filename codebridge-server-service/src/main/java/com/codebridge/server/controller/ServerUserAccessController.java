package com.codebridge.server.controller;

import com.codebridge.server.dto.ServerUserRequest;
import com.codebridge.server.dto.ServerUserResponse;
import com.codebridge.server.service.ServerAccessControlService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List; // Required for potential list grants endpoint
import java.util.UUID;

@RestController
@RequestMapping("/api/servers/{serverId}/access")
public class ServerUserAccessController {

    private final ServerAccessControlService serverAccessControlService;

    public ServerUserAccessController(ServerAccessControlService serverAccessControlService) {
        this.serverAccessControlService = serverAccessControlService;
    }

    private UUID getPlatformUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("Authentication principal not found or username is null.");
        }
        return UUID.fromString(authentication.getName());
    }

    @PostMapping("/grants")
    public ResponseEntity<ServerUserResponse> grantServerAccess(
            @PathVariable UUID serverId,
            @Valid @RequestBody ServerUserRequest serverUserRequest,
            Authentication authentication) {
        UUID adminUserId = getPlatformUserId(authentication); // User performing the grant action
        ServerUserResponse response = serverAccessControlService.grantServerAccess(adminUserId, serverId, serverUserRequest);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @DeleteMapping("/grants/{granteePlatformUserId}")
    public ResponseEntity<Void> revokeServerAccess(
            @PathVariable UUID serverId,
            @PathVariable UUID granteePlatformUserId, // The user whose access is being revoked
            Authentication authentication) {
        UUID adminUserId = getPlatformUserId(authentication); // User performing the revoke action
        serverAccessControlService.revokeServerAccess(adminUserId, serverId, granteePlatformUserId);
        return ResponseEntity.noContent().build();
    }
    
    // Optional: Endpoint to list users who have access to a server (for admin)
    // This would require a new method in ServerAccessControlService like `listGrantedUsersForServer(UUID serverId, UUID adminUserId)`
    /*
    @GetMapping("/grants")
    public ResponseEntity<List<ServerUserResponse>> listServerGrants(
            @PathVariable UUID serverId,
            Authentication authentication) {
        UUID adminUserId = getPlatformUserId(authentication);
        // List<ServerUserResponse> grants = serverAccessControlService.listGrantedUsersForServer(serverId, adminUserId);
        // return ResponseEntity.ok(grants);
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build(); // Placeholder
    }
    */
}
