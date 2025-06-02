package com.codebridge.server.controller;

import com.codebridge.server.dto.ServerRequest;
import com.codebridge.server.dto.ServerResponse;
import com.codebridge.server.service.ServerManagementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication; // New import
import org.springframework.web.bind.annotation.*;
// import org.springframework.security.core.annotation.AuthenticationPrincipal;
// import org.springframework.security.core.userdetails.UserDetails; // Or your custom Principal object

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/servers")
public class ServerController {

    private final ServerManagementService serverManagementService;

    public ServerController(ServerManagementService serverManagementService) {
        this.serverManagementService = serverManagementService;
    }

    // getCurrentUserId() placeholder removed

    @PostMapping
    public ResponseEntity<ServerResponse> createServer(@Valid @RequestBody ServerRequest serverRequest, Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        ServerResponse response = serverManagementService.createServer(serverRequest, userId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{serverId}")
    public ResponseEntity<ServerResponse> getServerById(@PathVariable UUID serverId, Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        ServerResponse response = serverManagementService.getServerById(serverId, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<ServerResponse>> listServersForUser(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        List<ServerResponse> responses = serverManagementService.listServersForUser(userId);
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{serverId}")
    public ResponseEntity<ServerResponse> updateServer(@PathVariable UUID serverId,
                                                     @Valid @RequestBody ServerRequest serverRequest,
                                                     Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        ServerResponse response = serverManagementService.updateServer(serverId, serverRequest, userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{serverId}")
    public ResponseEntity<Void> deleteServer(@PathVariable UUID serverId, Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        serverManagementService.deleteServer(serverId, userId);
        return ResponseEntity.noContent().build();
    }
}
