package com.codebridge.server.controller;

import com.codebridge.server.dto.ServerRequest;
import com.codebridge.server.dto.ServerResponse;
import com.codebridge.server.service.ServerManagementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    // Placeholder for userId extraction - replace with actual Spring Security principal
    private UUID getCurrentUserId() {
        // In a real app with Spring Security:
        // UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        // return UUID.fromString(userDetails.getUsername()); // Assuming username is UUID
        // For now, using a placeholder. This MUST be replaced.
        return UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"); // Example UUID
    }

    @PostMapping
    public ResponseEntity<ServerResponse> createServer(@Valid @RequestBody ServerRequest serverRequest) {
        UUID userId = getCurrentUserId(); // Replace
        ServerResponse response = serverManagementService.createServer(serverRequest, userId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{serverId}")
    public ResponseEntity<ServerResponse> getServerById(@PathVariable UUID serverId) {
        UUID userId = getCurrentUserId(); // Replace
        ServerResponse response = serverManagementService.getServerById(serverId, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<ServerResponse>> listServersForUser() {
        UUID userId = getCurrentUserId(); // Replace
        List<ServerResponse> responses = serverManagementService.listServersForUser(userId);
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{serverId}")
    public ResponseEntity<ServerResponse> updateServer(@PathVariable UUID serverId,
                                                     @Valid @RequestBody ServerRequest serverRequest) {
        UUID userId = getCurrentUserId(); // Replace
        ServerResponse response = serverManagementService.updateServer(serverId, serverRequest, userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{serverId}")
    public ResponseEntity<Void> deleteServer(@PathVariable UUID serverId) {
        UUID userId = getCurrentUserId(); // Replace
        serverManagementService.deleteServer(serverId, userId);
        return ResponseEntity.noContent().build();
    }
}
