package com.codebridge.documentation.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing an API example.
 */
@Entity
@Table(name = "api_examples", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"documentation_id", "operation_id"})
})
@Data
@NoArgsConstructor
public class ApiExample {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "documentation_id", nullable = false)
    private ApiDocumentation documentation;

    @Column(name = "operation_id", nullable = false)
    private String operationId;

    @Column(name = "path", nullable = false)
    private String path;

    @Column(name = "method", nullable = false)
    private String method;

    @Column(name = "request_example", columnDefinition = "TEXT")
    private String requestExample;

    @Column(name = "response_example", columnDefinition = "TEXT")
    private String responseExample;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

