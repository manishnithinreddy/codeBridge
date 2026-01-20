package com.codebridge.monitoring.scalability.resilience;

import java.util.List;

/**
 * Service for managing data replication.
 */
public interface ReplicationService {

    /**
     * Replicates data to all replicas.
     *
     * @param sql the SQL statement to replicate
     * @param params the parameters for the SQL statement
     * @return true if the replication was successful, false otherwise
     */
    boolean replicateData(String sql, Object... params);

    /**
     * Gets a connection to a replica for read operations.
     *
     * @return the name of the replica to use
     */
    String getReplicaForRead();

    /**
     * Gets all available replicas.
     *
     * @return a list of replica names
     */
    List<String> getAvailableReplicas();

    /**
     * Checks if a replica is healthy.
     *
     * @param replicaName the name of the replica
     * @return true if the replica is healthy, false otherwise
     */
    boolean isReplicaHealthy(String replicaName);

    /**
     * Gets the current consistency level.
     *
     * @return the consistency level
     */
    String getConsistencyLevel();

    /**
     * Sets the consistency level.
     *
     * @param consistencyLevel the consistency level
     */
    void setConsistencyLevel(String consistencyLevel);
}

