package com.codebridge.aidb.db.repository;

import com.codebridge.aidb.db.model.DatabaseConnection;
import com.codebridge.aidb.db.model.DatabaseSchema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing database schemas.
 */
@Repository
public interface DatabaseSchemaRepository extends JpaRepository<DatabaseSchema, UUID> {

    /**
     * Find all schemas for a specific connection.
     *
     * @param connection the database connection
     * @return list of database schemas
     */
    List<DatabaseSchema> findByConnection(DatabaseConnection connection);

    /**
     * Find all schemas for a specific connection ID.
     *
     * @param connectionId the database connection ID
     * @return list of database schemas
     */
    List<DatabaseSchema> findByConnectionId(UUID connectionId);

    /**
     * Find the latest schema for a specific connection.
     *
     * @param connection the database connection
     * @return the latest database schema if found
     */
    Optional<DatabaseSchema> findTopByConnectionOrderByCapturedAtDesc(DatabaseConnection connection);

    /**
     * Find a schema by connection and version.
     *
     * @param connection the database connection
     * @param version the schema version
     * @return the database schema if found
     */
    Optional<DatabaseSchema> findByConnectionAndVersion(DatabaseConnection connection, String version);
}

