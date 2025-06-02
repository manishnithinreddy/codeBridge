package com.codebridge.session.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

// This DTO is expected to be part of the request body for initializing an SSH session.
// It's provided by an authorized calling service (like codebridge-server-service)
// which has already fetched and decrypted necessary credentials.
public class UserProvidedConnectionDetails {

    @NotBlank(message = "Hostname cannot be blank.")
    private String hostname;

    @NotNull(message = "Port cannot be null.")
    @Min(value = 1, message = "Port must be at least 1.")
    @Max(value = 65535, message = "Port must be at most 65535.")
    private int port;

    @NotBlank(message = "Username cannot be blank.")
    private String username;

    @NotNull(message = "Decrypted private key cannot be null.")
    // Assuming byte[] for private key to avoid encoding issues with String.
    // Alternatively, if String is used, ensure consistent encoding/decoding.
    private byte[] decryptedPrivateKey;

    private byte[] publicKey; // Optional, can be null

    // Default constructor for Jackson
    public UserProvidedConnectionDetails() {
    }

    public UserProvidedConnectionDetails(String hostname, int port, String username, byte[] decryptedPrivateKey, byte[] publicKey) {
        this.hostname = hostname;
        this.port = port;
        this.username = username;
        this.decryptedPrivateKey = decryptedPrivateKey;
        this.publicKey = publicKey;
    }

    // Getters
    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
    }

    public String getUsername() {
        return username;
    }

    public byte[] getDecryptedPrivateKey() {
        return decryptedPrivateKey;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    // Setters
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setDecryptedPrivateKey(byte[] decryptedPrivateKey) {
        this.decryptedPrivateKey = decryptedPrivateKey;
    }

    public void setPublicKey(byte[] publicKey) {
        this.publicKey = publicKey;
    }
}
