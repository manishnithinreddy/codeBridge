package com.codebridge.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class SshKeyRequest {

    @NotBlank(message = "Name cannot be blank")
    @Size(max = 255, message = "Name cannot exceed 255 characters")
    private String name;

    @Size(max = 4000, message = "Public key cannot exceed 4000 characters")
    private String publicKey; // Optional, could be derived from private key if not provided

    @NotBlank(message = "Private key cannot be blank")
    @Size(max = 8000, message = "Private key is too long") // Max size for typical PEM keys
    private String privateKey; // Plain text private key, will be encrypted in service layer

    // userId will be typically sourced from the authenticated user context in the service layer, not directly from request body for security.

    // Getters and Setters
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

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }
}
