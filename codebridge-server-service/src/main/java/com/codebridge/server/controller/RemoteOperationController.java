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

    // getCurrentUserId() removed as session token will provide user context

    @PostMapping("/execute-command")
    public ResponseEntity<CommandResponse> executeCommand(
            @PathVariable UUID serverId,
            @RequestHeader("X-Session-Token") String sessionToken,
            @Valid @RequestBody CommandRequest commandRequest) {
        // The platformUserId will be extracted from the sessionToken within the service layer
        CommandResponse response = remoteExecutionService.executeCommand(serverId, sessionToken, commandRequest);
        return ResponseEntity.ok(response);
    }
}
