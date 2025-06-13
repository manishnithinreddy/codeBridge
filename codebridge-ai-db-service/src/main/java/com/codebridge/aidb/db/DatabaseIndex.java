package com.codebridge.aidb.db.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.util.UUID;

/**
 * Entity representing a database index.
 */
@Entity
@Table(name = "database_indexes")
@Data
@NoArgsConstructor
public class DatabaseIndex {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(name = "table_id", nullable = false)
    private DatabaseTable table;

    @Column(nullable = false)
    private boolean unique;

    @Column(columnDefinition = "TEXT")
    private String columns;

    @Column
    private String type;

    @Column
    private String filterCondition;
}

