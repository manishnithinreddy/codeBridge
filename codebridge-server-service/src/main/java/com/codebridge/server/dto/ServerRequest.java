package com.codebridge.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.util.UUID;

public class ServerRequest {

    @NotBlank(message = "Server name cannot be blank")
    @Size(max = 255, message = "Server name cannot exceed 255 characters")
    private String name;

    @NotBlank(message = "Hostname cannot be blank")
    @Size(max = 255, message = "Hostname cannot exceed 255 characters")
    private String hostname;

    @NotNull(message = "Port cannot be null")
    @Min(value = 1, message = "Port number must be at least 1")
    @Max(value = 65535, message = "Port number cannot exceed 65535")
    private Integer port = 22;

    @NotBlank(message = "Remote username cannot be blank")
    @Size(max = 255, message = "Remote username cannot exceed 255 characters")
    private String remoteUsername;

    @NotBlank(message = "Auth provider cannot be blank. Choose SSH_KEY or PASSWORD.")
    private String authProvider; // Will be mapped to ServerAuthProvider enum

    @Size(max = 255, message = "Password is too long") // Assuming it's a placeholder or for temporary use if ever plain
    private String password; // Plain text if provided, to be encrypted in service layer

    private UUID sshKeyId; // ID of an existing SshKey entity

    // ServerStatus is typically managed by the system, not set directly by user in request.
    // operatingSystem and cloudProvider are optional.

    @Size(max = 255, message = "Operating system name is too long")
    private String operatingSystem;
    
    private String cloudProvider; // Will be mapped to ServerCloudProvider enum

    // userId will be sourced from authenticated context

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getRemoteUsername() {
        return remoteUsername;
    }

    public void setRemoteUsername(String remoteUsername) {
        this.remoteUsername = remoteUsername;
    }

    public String getAuthProvider() {
        return authProvider;
    }

    public void setAuthProvider(String authProvider) {
        this.authProvider = authProvider;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public UUID getSshKeyId() {
        return sshKeyId;
    }

    public void setSshKeyId(UUID sshKeyId) {
        this.sshKeyId = sshKeyId;
    }

    public String getOperatingSystem() {
        return operatingSystem;
    }

    public void setOperatingSystem(String operatingSystem) {
        this.operatingSystem = operatingSystem;
    }

    public String getCloudProvider() {
        return cloudProvider;
    }

    public void setCloudProvider(String cloudProvider) {
        this.cloudProvider = cloudProvider;
    }
}
