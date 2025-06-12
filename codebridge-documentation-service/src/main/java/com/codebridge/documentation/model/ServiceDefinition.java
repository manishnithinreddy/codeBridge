package com.codebridge.documentation.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing a service definition.
 */
@Entity
@Table(name = "service_definitions")
@Data
@NoArgsConstructor
public class ServiceDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "url", nullable = false)
    private String url;

    @Column(name = "context_path")
    private String contextPath;

    @Column(name = "scan", nullable = false)
    private boolean scan;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

