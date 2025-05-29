package com.codebridge.server.controller;

import com.codebridge.server.dto.remote.CommandRequest;
import com.codebridge.server.dto.remote.CommandResponse;
import com.codebridge.server.service.RemoteExecutionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/servers/{serverId}/remote")
public class RemoteOperationController {

    private final RemoteExecutionService remoteExecutionService;

    public RemoteOperationController(RemoteExecutionService remoteExecutionService) {
        this.remoteExecutionService = remoteExecutionService;
    }

    // Placeholder for userId extraction - replace with actual Spring Security principal
    private UUID getCurrentUserId() {
        // In a real app with Spring Security:
        // UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        // return UUID.fromString(userDetails.getUsername()); // Assuming username is UUID
        // For now, using a placeholder. This MUST be replaced.
        return UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"); // Example UUID
    }

    @PostMapping("/execute-command")
    public ResponseEntity<CommandResponse> executeCommand(
            @PathVariable UUID serverId,
            @Valid @RequestBody CommandRequest commandRequest) {
        UUID userId = getCurrentUserId(); // Replace with actual user ID from security context
        CommandResponse response = remoteExecutionService.executeCommand(serverId, userId, commandRequest);
        return ResponseEntity.ok(response);
    }
}
