package org.springframework.data.redis.connection;

/**
 * Interface for a factory that creates Redis connections.
 * This is a simplified version for test purposes.
 */
public interface RedisConnectionFactory {
    /**
     * Get a connection.
     * @return a connection
     */
    Object getConnection();
    
    /**
     * Get a connection for a specific database.
     * @param database the database
     * @return a connection
     */
    Object getConnection(String database);
    
    /**
     * Get a cluster connection.
     * @return a cluster connection factory
     */
    RedisConnectionFactory getClusterConnection();
    
    /**
     * Whether to convert pipeline and transaction results.
     * @return true if pipeline and transaction results should be converted
     */
    boolean getConvertPipelineAndTxResults();
}

