package com.codebridge.aidb.db.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing a database schema.
 */
@Entity
@Table(name = "database_schemas")
@Data
@NoArgsConstructor
public class DatabaseSchema {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    @ManyToOne
    @JoinColumn(name = "connection_id", nullable = false)
    private DatabaseConnection connection;

    @Column(nullable = false)
    private LocalDateTime capturedAt;

    @Column(nullable = false)
    private String version;

    @OneToMany(mappedBy = "schema", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DatabaseTable> tables = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String rawSchema;

    @PrePersist
    protected void onCreate() {
        capturedAt = LocalDateTime.now();
    }
}

