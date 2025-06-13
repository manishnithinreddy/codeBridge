package com.codebridge.documentation.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing a code sample.
 */
@Entity
@Table(name = "code_samples", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"documentation_id", "operation_id", "language"})
})
@Data
@NoArgsConstructor
public class CodeSample {

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

    @Enumerated(EnumType.STRING)
    @Column(name = "language", nullable = false)
    private ProgrammingLanguage language;

    @Column(name = "code", columnDefinition = "TEXT", nullable = false)
    private String code;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

