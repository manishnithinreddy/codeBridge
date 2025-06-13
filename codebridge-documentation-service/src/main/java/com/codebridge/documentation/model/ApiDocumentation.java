package com.codebridge.documentation.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing API documentation.
 */
@Entity
@Table(name = "api_documentation", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"service_id", "version_id"})
})
@Data
@NoArgsConstructor
public class ApiDocumentation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceDefinition service;

    @ManyToOne
    @JoinColumn(name = "version_id", nullable = false)
    private ApiVersion version;

    @Enumerated(EnumType.STRING)
    @Column(name = "format", nullable = false)
    private DocumentationFormat format;

    @Column(name = "open_api_spec", columnDefinition = "TEXT", nullable = false)
    private String openApiSpec;

    @Column(name = "open_api_path")
    private String openApiPath;

    @Column(name = "html_path")
    private String htmlPath;

    @Column(name = "markdown_path")
    private String markdownPath;

    @Enumerated(EnumType.STRING)
    @Column(name = "publish_status")
    private PublishStatus publishStatus;

    @Column(name = "publish_error")
    private String publishError;

    @Column(name = "last_published_at")
    private Instant lastPublishedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

