package com.codebridge.server.controller;

import com.codebridge.server.dto.ServerRequest;
import com.codebridge.server.dto.ServerResponse;
import com.codebridge.server.service.ServerManagementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/servers")
public class ServerController {

    private final ServerManagementService serverManagementService;

    public ServerController(ServerManagementService serverManagementService) {
        this.serverManagementService = serverManagementService;
    }

    private UUID getPlatformUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("Authentication principal not found or username is null.");
        }
        return UUID.fromString(authentication.getName());
    }

    @PostMapping
    public ResponseEntity<ServerResponse> createServer(@Valid @RequestBody ServerRequest serverRequest,
                                                       Authentication authentication) {
        UUID platformUserId = getPlatformUserId(authentication);
        ServerResponse createdServer = serverManagementService.createServer(serverRequest, platformUserId);
        return new ResponseEntity<>(createdServer, HttpStatus.CREATED);
    }

    @GetMapping("/{serverId}")
    public ResponseEntity<ServerResponse> getServerById(@PathVariable UUID serverId,
                                                        Authentication authentication) {
        UUID platformUserId = getPlatformUserId(authentication);
        ServerResponse server = serverManagementService.getServerById(serverId, platformUserId);
        return ResponseEntity.ok(server);
    }

    @GetMapping
    public ResponseEntity<List<ServerResponse>> listServers(Authentication authentication) {
        UUID platformUserId = getPlatformUserId(authentication);
        List<ServerResponse> servers = serverManagementService.listServersForUser(platformUserId);
        return ResponseEntity.ok(servers);
    }

    @PutMapping("/{serverId}")
    public ResponseEntity<ServerResponse> updateServer(@PathVariable UUID serverId,
                                                       @Valid @RequestBody ServerRequest serverRequest,
                                                       Authentication authentication) {
        UUID platformUserId = getPlatformUserId(authentication);
        ServerResponse updatedServer = serverManagementService.updateServer(serverId, serverRequest, platformUserId);
        return ResponseEntity.ok(updatedServer);
    }

    @DeleteMapping("/{serverId}")
    public ResponseEntity<Void> deleteServer(@PathVariable UUID serverId,
                                             Authentication authentication) {
        UUID platformUserId = getPlatformUserId(authentication);
        serverManagementService.deleteServer(serverId, platformUserId);
        return ResponseEntity.noContent().build();
    }
}
