package com.codebridge.server.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "server_activity_logs")
public class ServerActivityLog {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_id") // Nullable if activity is not server-specific
    private Server server;

    @Column(nullable = false)
    private UUID platformUserId; // Who performed the action

    @Column(nullable = false)
    private String action; // e.g., "EXECUTED_COMMAND", "UPLOADED_FILE", "GRANTED_ACCESS"

    @Lob
    @Column(columnDefinition = "TEXT")
    private String details; // e.g., the command executed, file path, user granted access to

    @Column(nullable = false)
    private String status; // e.g., "SUCCESS", "FAILURE"

    @Lob
    @Column(columnDefinition = "TEXT")
    private String errorMessage; // if status is "FAILURE"

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        id = UUID.randomUUID();
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Server getServer() {
        return server;
    }

    public void setServer(Server server) {
        this.server = server;
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

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
