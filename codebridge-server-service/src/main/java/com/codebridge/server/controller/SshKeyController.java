package com.codebridge.server.controller;

import com.codebridge.server.dto.SshKeyRequest;
import com.codebridge.server.dto.SshKeyResponse;
import com.codebridge.server.service.SshKeyManagementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    // Placeholder for userId extraction - replace with actual Spring Security principal
    private UUID getCurrentUserId() {
        // In a real app with Spring Security:
        // UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        // return UUID.fromString(userDetails.getUsername()); // Assuming username is UUID
        // For now, using a placeholder. This MUST be replaced.
        return UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"); // Example UUID
    }

    @PostMapping
    public ResponseEntity<SshKeyResponse> createSshKey(@Valid @RequestBody SshKeyRequest sshKeyRequest) {
        UUID userId = getCurrentUserId(); // Replace with actual user ID from security context
        SshKeyResponse response = sshKeyManagementService.createSshKey(sshKeyRequest, userId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{keyId}")
    public ResponseEntity<SshKeyResponse> getSshKeyById(@PathVariable UUID keyId) {
        UUID userId = getCurrentUserId(); // Replace
        SshKeyResponse response = sshKeyManagementService.getSshKeyById(keyId, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<SshKeyResponse>> listSshKeysForUser() {
        UUID userId = getCurrentUserId(); // Replace
        List<SshKeyResponse> responses = sshKeyManagementService.listSshKeysForUser(userId);
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/{keyId}")
    public ResponseEntity<Void> deleteSshKey(@PathVariable UUID keyId) {
        UUID userId = getCurrentUserId(); // Replace
        sshKeyManagementService.deleteSshKey(keyId, userId);
        return ResponseEntity.noContent().build();
    }
}
