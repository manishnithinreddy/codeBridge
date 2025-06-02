package com.codebridge.server.controller;

import com.codebridge.server.dto.remote.CommandRequest;
import com.codebridge.server.dto.remote.CommandResponse;
import com.codebridge.server.service.RemoteExecutionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/servers/{serverId}/remote")
public class RemoteOperationController {

    private final RemoteExecutionService remoteExecutionService;

    public RemoteOperationController(RemoteExecutionService remoteExecutionService) {
        this.remoteExecutionService = remoteExecutionService;
    }

    private UUID getPlatformUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            // This should ideally be caught by security filters if endpoint is protected
            throw new IllegalStateException("Authentication principal not found or username is null.");
        }
        return UUID.fromString(authentication.getName());
    }

    @PostMapping("/execute-command")
    public ResponseEntity<CommandResponse> executeCommand(
            @PathVariable UUID serverId,
            @Valid @RequestBody CommandRequest commandRequest,
            Authentication authentication) {
        UUID platformUserId = getPlatformUserId(authentication);
        CommandResponse response = remoteExecutionService.executeCommand(serverId, platformUserId, commandRequest);
        return ResponseEntity.ok(response);
    }
}
