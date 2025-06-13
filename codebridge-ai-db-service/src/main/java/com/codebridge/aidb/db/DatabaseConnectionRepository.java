package com.codebridge.aidb.db.repository;

import com.codebridge.aidb.db.model.DatabaseConnection;
import com.codebridge.aidb.db.model.DatabaseType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing database connections.
 */
@Repository
public interface DatabaseConnectionRepository extends JpaRepository<DatabaseConnection, UUID> {

    /**
     * Find a database connection by name.
     *
     * @param name the name of the connection
     * @return the database connection if found
     */
    Optional<DatabaseConnection> findByName(String name);

    /**
     * Find all database connections of a specific type.
     *
     * @param type the database type
     * @return list of database connections
     */
    List<DatabaseConnection> findByType(DatabaseType type);

    /**
     * Find all enabled database connections.
     *
     * @return list of enabled database connections
     */
    List<DatabaseConnection> findByEnabledTrue();

    /**
     * Find all enabled database connections of a specific type.
     *
     * @param type the database type
     * @return list of enabled database connections of the specified type
     */
    List<DatabaseConnection> findByTypeAndEnabledTrue(DatabaseType type);
}

