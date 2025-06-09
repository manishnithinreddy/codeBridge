package com.codebridge.server.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp; // Using CreationTimestamp for automatic timestamping

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Objects;

@Entity
@Table(name = "server_activity_logs")
public class ServerActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "server_id") // Nullable if the activity is not server-specific (e.g. SSH key creation)
    private UUID serverId;

    @NotNull
    @Column(name = "platform_user_id", nullable = false)
    private UUID platformUserId; // Platform user who performed or initiated the action

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false)
    private String action; // e.g., "SERVER_CREATE", "SSH_CONNECT", "FILE_UPLOAD"

    @Lob
    @Column(columnDefinition = "TEXT")
    private String details; // JSON or textual details of the action

    @NotBlank
    @Size(max = 50) // e.g., SUCCESS, FAILED, IN_PROGRESS
    @Column(nullable = false)
    private String status;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String errorMessage; // Nullable
    
    @Size(max = 255)
    @Column
    private String ipAddress; // Client IP address
    
    @Size(max = 1024)
    @Column
    private String userAgent; // Client user agent

    @CreationTimestamp // Sets timestamp automatically on creation
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    // Constructors
    public ServerActivityLog() {
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getServerId() {
        return serverId;
    }

    public void setServerId(UUID serverId) {
        this.serverId = serverId;
    }

    public UUID getPlatformUserId() {
        return platformUserId;
    }

    public void setPlatformUserId(UUID platformUserId) {
        this.platformUserId = platformUserId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServerActivityLog that = (ServerActivityLog) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ServerActivityLog{" +
               "id=" + id +
               ", serverId=" + serverId +
               ", platformUserId=" + platformUserId +
               ", action='" + action + '\'' +
               ", status='" + status + '\'' +
               ", ipAddress='" + ipAddress + '\'' +
               ", timestamp=" + timestamp +
               '}';
    }
}
