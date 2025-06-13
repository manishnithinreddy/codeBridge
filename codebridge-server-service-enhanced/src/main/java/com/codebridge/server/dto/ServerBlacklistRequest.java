package com.codebridge.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * DTO for server blacklist request.
 */
public class ServerBlacklistRequest {

    @NotBlank(message = "IP address is required")
    @Size(max = 255, message = "IP address must be less than 255 characters")
    private String ipAddress;

    @Size(max = 255, message = "Hostname must be less than 255 characters")
    private String hostname;

    @Size(max = 255, message = "Reason must be less than 255 characters")
    private String reason;

    private LocalDateTime expiresAt;

    // Default constructor
    public ServerBlacklistRequest() {
    }

    // Constructor with required fields
    public ServerBlacklistRequest(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    // Getters and setters
    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}

