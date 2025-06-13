package com.codebridge.db.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing a database table.
 */
@Entity
@Table(name = "database_tables")
@Data
@NoArgsConstructor
public class DatabaseTable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    @ManyToOne
    @JoinColumn(name = "schema_id", nullable = false)
    private DatabaseSchema schema;

    @OneToMany(mappedBy = "table", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DatabaseColumn> columns = new ArrayList<>();

    @OneToMany(mappedBy = "table", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DatabaseIndex> indexes = new ArrayList<>();

    @Column
    private Long rowCount;

    @Column
    private Long sizeBytes;

    @Column
    private String tableType;
}

