package com.codebridge.server.controller;

import com.codebridge.server.dto.SshKeyRequest;
import com.codebridge.server.dto.SshKeyResponse;
import com.codebridge.server.service.SshKeyManagementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ssh-keys")
public class SshKeyController {

    private final SshKeyManagementService sshKeyManagementService;

    public SshKeyController(SshKeyManagementService sshKeyManagementService) {
        this.sshKeyManagementService = sshKeyManagementService;
    }

    private UUID getPlatformUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("Authentication principal not found or username is null.");
        }
        return UUID.fromString(authentication.getName());
    }

    @PostMapping
    public ResponseEntity<SshKeyResponse> createSshKey(@Valid @RequestBody SshKeyRequest sshKeyRequest,
                                                       Authentication authentication) {
        UUID platformUserId = getPlatformUserId(authentication);
        SshKeyResponse createdKey = sshKeyManagementService.createSshKey(sshKeyRequest, platformUserId);
        return new ResponseEntity<>(createdKey, HttpStatus.CREATED);
    }

    @GetMapping("/{keyId}")
    public ResponseEntity<SshKeyResponse> getSshKeyById(@PathVariable UUID keyId,
                                                        Authentication authentication) {
        UUID platformUserId = getPlatformUserId(authentication);
        SshKeyResponse sshKey = sshKeyManagementService.getSshKeyById(keyId, platformUserId);
        return ResponseEntity.ok(sshKey);
    }

    @GetMapping
    public ResponseEntity<List<SshKeyResponse>> listSshKeys(Authentication authentication) {
        UUID platformUserId = getPlatformUserId(authentication);
        List<SshKeyResponse> keys = sshKeyManagementService.listSshKeysForUser(platformUserId);
        return ResponseEntity.ok(keys);
    }

    @DeleteMapping("/{keyId}")
    public ResponseEntity<Void> deleteSshKey(@PathVariable UUID keyId,
                                             Authentication authentication) {
        UUID platformUserId = getPlatformUserId(authentication);
        sshKeyManagementService.deleteSshKey(keyId, platformUserId);
        return ResponseEntity.noContent().build();
    }
}
