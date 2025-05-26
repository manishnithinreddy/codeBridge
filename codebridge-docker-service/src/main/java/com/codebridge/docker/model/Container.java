package com.codebridge.docker.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a Docker container.
 */
@Entity
@Table(name = "containers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Container {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "container_id", nullable = false, unique = true)
    private String containerId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "team_id")
    private UUID teamId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "image_name", nullable = false)
    private String imageName;

    @Column(name = "image_tag")
    private String imageTag;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ContainerStatus status;

    @Column(name = "port_mappings")
    private String portMappings;

    @Column(name = "environment_variables")
    private String environmentVariables;

    @Column(name = "resource_limits")
    private String resourceLimits;

    @Column(name = "last_started")
    private LocalDateTime lastStarted;

    @Column(name = "last_stopped")
    private LocalDateTime lastStopped;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

