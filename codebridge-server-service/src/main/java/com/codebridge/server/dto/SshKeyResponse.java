package com.codebridge.server.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class SshKeyResponse {
    private UUID id;
    private String name;
    private String publicKey;
    private String fingerprint;
    private UUID userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructor, Getters, Setters
    public SshKeyResponse() {
    }

    public SshKeyResponse(UUID id, String name, String publicKey, String fingerprint, UUID userId, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.publicKey = publicKey;
        this.fingerprint = fingerprint;
        this.userId = userId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
