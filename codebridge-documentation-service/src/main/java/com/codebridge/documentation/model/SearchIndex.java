package com.codebridge.documentation.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing a search index entry.
 */
@Entity
@Table(name = "search_index")
@Data
@NoArgsConstructor
public class SearchIndex {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "documentation_id", nullable = false)
    private ApiDocumentation documentation;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private SearchIndexType type;

    @Column(name = "title")
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "path")
    private String path;

    @Column(name = "method")
    private String method;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}

