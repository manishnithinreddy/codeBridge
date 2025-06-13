package com.codebridge.db.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.util.UUID;

/**
 * Entity representing a database column.
 */
@Entity
@Table(name = "database_columns")
@Data
@NoArgsConstructor
public class DatabaseColumn {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    @ManyToOne
    @JoinColumn(name = "table_id", nullable = false)
    private DatabaseTable table;

    @Column(nullable = false)
    private String dataType;

    @Column
    private Integer position;

    @Column
    private boolean nullable;

    @Column
    private boolean primaryKey;

    @Column
    private boolean foreignKey;

    @Column
    private String defaultValue;

    @Column
    private Integer maxLength;

    @Column
    private Integer precision;

    @Column
    private Integer scale;

    @Column
    private String referencedTable;

    @Column
    private String referencedColumn;
}

