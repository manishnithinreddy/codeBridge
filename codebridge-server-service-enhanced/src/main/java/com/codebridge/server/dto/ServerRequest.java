package com.codebridge.server.dto;

import com.codebridge.server.model.enums.ServerAuthProvider;
import com.codebridge.server.model.enums.ServerCloudProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.util.UUID;

public class ServerRequest {

    @NotBlank(message = "Server name cannot be blank")
    @Size(max = 255)
    private String name;

    @NotBlank(message = "Hostname cannot be blank")
    @Size(max = 255)
    private String hostname;

    @NotNull(message = "Port cannot be null")
    @Min(1)
    @Max(65535)
    private Integer port;

    @NotBlank(message = "Remote username cannot be blank")
    @Size(max = 255)
    private String remoteUsername;

    @NotNull(message = "Authentication provider cannot be null")
    private ServerAuthProvider authProvider;

    // Password should be provided if authProvider is PASSWORD
    private String password; // Handled with care by service

    // SSH Key ID should be provided if authProvider is SSH_KEY
    private UUID sshKeyId;

    private String operatingSystem;
    private ServerCloudProvider cloudProvider;

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

    public ServerAuthProvider getAuthProvider() {
        return authProvider;
    }

    public void setAuthProvider(ServerAuthProvider authProvider) {
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

    public ServerCloudProvider getCloudProvider() {
        return cloudProvider;
    }

    public void setCloudProvider(ServerCloudProvider cloudProvider) {
        this.cloudProvider = cloudProvider;
    }
}
