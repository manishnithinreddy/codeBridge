package com.codebridge.session.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for SSH host keys
 */
public class HostKeyDTO {
    
    private UUID id;
    
    @NotBlank(message = "Hostname is required")
    private String hostname;
    
    @NotNull(message = "Port is required")
    private int port;
    
    @NotBlank(message = "Key type is required")
    private String keyType;
    
    @NotBlank(message = "Host key (Base64) is required")
    private String hostKeyBase64;
    
    @NotBlank(message = "Fingerprint is required")
    private String fingerprintSha256;
    
    private LocalDateTime firstSeen;
    
    private LocalDateTime lastVerified;
    
    // Default constructor for deserialization
    public HostKeyDTO() {
    }
    
    // Constructor with all fields
    public HostKeyDTO(UUID id, String hostname, int port, String keyType, 
                      String hostKeyBase64, String fingerprintSha256,
                      LocalDateTime firstSeen, LocalDateTime lastVerified) {
        this.id = id;
        this.hostname = hostname;
        this.port = port;
        this.keyType = keyType;
        this.hostKeyBase64 = hostKeyBase64;
        this.fingerprintSha256 = fingerprintSha256;
        this.firstSeen = firstSeen;
        this.lastVerified = lastVerified;
    }
    
    // Getters and setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public String getHostname() {
        return hostname;
    }
    
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }
    
    public int getPort() {
        return port;
    }
    
    public void setPort(int port) {
        this.port = port;
    }
    
    public String getKeyType() {
        return keyType;
    }
    
    public void setKeyType(String keyType) {
        this.keyType = keyType;
    }
    
    public String getHostKeyBase64() {
        return hostKeyBase64;
    }
    
    public void setHostKeyBase64(String hostKeyBase64) {
        this.hostKeyBase64 = hostKeyBase64;
    }
    
    public String getFingerprintSha256() {
        return fingerprintSha256;
    }
    
    public void setFingerprintSha256(String fingerprintSha256) {
        this.fingerprintSha256 = fingerprintSha256;
    }
    
    public LocalDateTime getFirstSeen() {
        return firstSeen;
    }
    
    public void setFirstSeen(LocalDateTime firstSeen) {
        this.firstSeen = firstSeen;
    }
    
    public LocalDateTime getLastVerified() {
        return lastVerified;
    }
    
    public void setLastVerified(LocalDateTime lastVerified) {
        this.lastVerified = lastVerified;
    }
}

