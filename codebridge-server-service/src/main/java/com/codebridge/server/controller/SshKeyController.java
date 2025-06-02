package com.codebridge.server.controller;

import com.codebridge.server.dto.SshKeyRequest;
import com.codebridge.server.dto.SshKeyResponse;
import com.codebridge.server.service.SshKeyManagementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication; // New import
import org.springframework.web.bind.annotation.*;
// import org.springframework.security.core.annotation.AuthenticationPrincipal;
// import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ssh-keys")
public class SshKeyController {

    private final SshKeyManagementService sshKeyManagementService;

    public SshKeyController(SshKeyManagementService sshKeyManagementService) {
        this.sshKeyManagementService = sshKeyManagementService;
    }

    // getCurrentUserId() placeholder removed

    @PostMapping
    public ResponseEntity<SshKeyResponse> createSshKey(@Valid @RequestBody SshKeyRequest sshKeyRequest, Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        SshKeyResponse response = sshKeyManagementService.createSshKey(sshKeyRequest, userId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{keyId}")
    public ResponseEntity<SshKeyResponse> getSshKeyById(@PathVariable UUID keyId, Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        SshKeyResponse response = sshKeyManagementService.getSshKeyById(keyId, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<SshKeyResponse>> listSshKeysForUser(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        List<SshKeyResponse> responses = sshKeyManagementService.listSshKeysForUser(userId);
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/{keyId}")
    public ResponseEntity<Void> deleteSshKey(@PathVariable UUID keyId, Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        sshKeyManagementService.deleteSshKey(keyId, userId);
        return ResponseEntity.noContent().build();
    }
}
