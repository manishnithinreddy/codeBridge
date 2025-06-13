package com.codebridge.scalability.resilience;

import java.util.List;
import java.util.Map;

/**
 * Service for managing data partitioning.
 */
public interface DataPartitioningService {

    /**
     * Gets the shard for a given key.
     *
     * @param key the key to get the shard for
     * @return the shard ID
     */
    int getShardForKey(String key);

    /**
     * Executes a query on a specific shard.
     *
     * @param shardId the shard ID
     * @param sql the SQL statement to execute
     * @param params the parameters for the SQL statement
     * @return the query results
     */
    List<Map<String, Object>> executeQueryOnShard(int shardId, String sql, Object... params);

    /**
     * Executes a query on all shards and aggregates the results.
     *
     * @param sql the SQL statement to execute
     * @param params the parameters for the SQL statement
     * @return the aggregated query results
     */
    List<Map<String, Object>> executeQueryOnAllShards(String sql, Object... params);

    /**
     * Executes an update on a specific shard.
     *
     * @param shardId the shard ID
     * @param sql the SQL statement to execute
     * @param params the parameters for the SQL statement
     * @return the number of rows affected
     */
    int executeUpdateOnShard(int shardId, String sql, Object... params);

    /**
     * Executes an update on all shards.
     *
     * @param sql the SQL statement to execute
     * @param params the parameters for the SQL statement
     * @return the total number of rows affected
     */
    int executeUpdateOnAllShards(String sql, Object... params);

    /**
     * Rebalances data across shards.
     *
     * @return true if the rebalancing was successful, false otherwise
     */
    boolean rebalanceShards();

    /**
     * Gets information about all shards.
     *
     * @return a list of shard information
     */
    List<ShardInfo> getShardInfo();

    /**
     * Represents information about a shard.
     */
    class ShardInfo {
        private int shardId;
        private String connectionUrl;
        private long recordCount;
        private long sizeBytes;
        private String status; // ONLINE, OFFLINE, REBALANCING
        
        // Getters and setters
        public int getShardId() { return shardId; }
        public void setShardId(int shardId) { this.shardId = shardId; }
        
        public String getConnectionUrl() { return connectionUrl; }
        public void setConnectionUrl(String connectionUrl) { this.connectionUrl = connectionUrl; }
        
        public long getRecordCount() { return recordCount; }
        public void setRecordCount(long recordCount) { this.recordCount = recordCount; }
        
        public long getSizeBytes() { return sizeBytes; }
        public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}

