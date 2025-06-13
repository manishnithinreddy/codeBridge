package com.codebridge.scalability.resilience.impl;

import com.codebridge.scalability.resilience.DataPartitioningService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Default implementation of the data partitioning service.
 */
@Slf4j
public class DefaultDataPartitioningService implements DataPartitioningService {

    private final boolean enabled;
    private final String strategy;
    private final int shardCount;
    private final JdbcTemplate jdbcTemplate;
    
    private final Map<Integer, ShardInfo> shards = new ConcurrentHashMap<>();

    /**
     * Creates a new DefaultDataPartitioningService.
     *
     * @param enabled whether partitioning is enabled
     * @param strategy the partitioning strategy
     * @param shardCount the number of shards
     * @param jdbcTemplate the JDBC template
     */
    public DefaultDataPartitioningService(boolean enabled, String strategy, int shardCount, JdbcTemplate jdbcTemplate) {
        this.enabled = enabled;
        this.strategy = strategy;
        this.shardCount = shardCount;
        this.jdbcTemplate = jdbcTemplate;
        
        // Initialize shards
        if (enabled) {
            initializeShards();
        }
    }

    @Override
    public int getShardForKey(String key) {
        if (!enabled) {
            return 0; // Use primary shard
        }
        
        switch (strategy.toLowerCase()) {
            case "hash":
                return Math.abs(key.hashCode() % shardCount);
            case "range":
                // For demonstration, we'll use the first character of the key
                if (key.isEmpty()) {
                    return 0;
                }
                
                char firstChar = key.charAt(0);
                return Math.abs(firstChar % shardCount);
            case "list":
                // For demonstration, we'll use the first character of the key
                if (key.isEmpty()) {
                    return 0;
                }
                
                char c = key.charAt(0);
                
                if (Character.isDigit(c)) {
                    return 0;
                } else if (Character.isLowerCase(c)) {
                    return 1 % shardCount;
                } else {
                    return 2 % shardCount;
                }
            default:
                return 0;
        }
    }

    @Override
    public List<Map<String, Object>> executeQueryOnShard(int shardId, String sql, Object... params) {
        if (!enabled) {
            // Execute on primary database
            return jdbcTemplate.queryForList(sql, params);
        }
        
        ShardInfo shard = shards.get(shardId);
        
        if (shard == null) {
            throw new IllegalArgumentException("Shard not found: " + shardId);
        }
        
        if (!"ONLINE".equals(shard.getStatus())) {
            throw new IllegalStateException("Shard is not online: " + shardId);
        }
        
        // In a real implementation, this would use a connection to the shard
        // For demonstration, we'll just execute on the primary database
        log.info("Executing query on shard {}: {}", shardId, sql);
        
        return jdbcTemplate.queryForList(sql, params);
    }

    @Override
    public List<Map<String, Object>> executeQueryOnAllShards(String sql, Object... params) {
        if (!enabled) {
            // Execute on primary database
            return jdbcTemplate.queryForList(sql, params);
        }
        
        List<Map<String, Object>> results = new ArrayList<>();
        
        for (int shardId : shards.keySet()) {
            ShardInfo shard = shards.get(shardId);
            
            if ("ONLINE".equals(shard.getStatus())) {
                try {
                    List<Map<String, Object>> shardResults = executeQueryOnShard(shardId, sql, params);
                    results.addAll(shardResults);
                } catch (Exception e) {
                    log.error("Failed to execute query on shard {}: {}", shardId, e.getMessage());
                }
            }
        }
        
        return results;
    }

    @Override
    public int executeUpdateOnShard(int shardId, String sql, Object... params) {
        if (!enabled) {
            // Execute on primary database
            return jdbcTemplate.update(sql, params);
        }
        
        ShardInfo shard = shards.get(shardId);
        
        if (shard == null) {
            throw new IllegalArgumentException("Shard not found: " + shardId);
        }
        
        if (!"ONLINE".equals(shard.getStatus())) {
            throw new IllegalStateException("Shard is not online: " + shardId);
        }
        
        // In a real implementation, this would use a connection to the shard
        // For demonstration, we'll just execute on the primary database
        log.info("Executing update on shard {}: {}", shardId, sql);
        
        return jdbcTemplate.update(sql, params);
    }

    @Override
    public int executeUpdateOnAllShards(String sql, Object... params) {
        if (!enabled) {
            // Execute on primary database
            return jdbcTemplate.update(sql, params);
        }
        
        int totalRowsAffected = 0;
        
        for (int shardId : shards.keySet()) {
            ShardInfo shard = shards.get(shardId);
            
            if ("ONLINE".equals(shard.getStatus())) {
                try {
                    int rowsAffected = executeUpdateOnShard(shardId, sql, params);
                    totalRowsAffected += rowsAffected;
                } catch (Exception e) {
                    log.error("Failed to execute update on shard {}: {}", shardId, e.getMessage());
                }
            }
        }
        
        return totalRowsAffected;
    }

    @Override
    public boolean rebalanceShards() {
        if (!enabled) {
            return true;
        }
        
        try {
            log.info("Rebalancing shards");
            
            // Mark all shards as rebalancing
            for (ShardInfo shard : shards.values()) {
                shard.setStatus("REBALANCING");
            }
            
            // In a real implementation, this would rebalance data across shards
            // For demonstration, we'll just update the record counts
            long totalRecords = shards.values().stream()
                    .mapToLong(ShardInfo::getRecordCount)
                    .sum();
            
            long recordsPerShard = totalRecords / shardCount;
            long remainder = totalRecords % shardCount;
            
            for (int i = 0; i < shardCount; i++) {
                ShardInfo shard = shards.get(i);
                
                if (shard != null) {
                    shard.setRecordCount(recordsPerShard + (i < remainder ? 1 : 0));
                    shard.setStatus("ONLINE");
                }
            }
            
            log.info("Shards rebalanced");
            
            return true;
        } catch (Exception e) {
            log.error("Failed to rebalance shards: {}", e.getMessage());
            
            // Mark all shards as online
            for (ShardInfo shard : shards.values()) {
                shard.setStatus("ONLINE");
            }
            
            return false;
        }
    }

    @Override
    public List<ShardInfo> getShardInfo() {
        return new ArrayList<>(shards.values());
    }
    
    private void initializeShards() {
        // Create shard info objects
        IntStream.range(0, shardCount).forEach(i -> {
            ShardInfo shard = new ShardInfo();
            shard.setShardId(i);
            shard.setConnectionUrl("jdbc:postgresql://shard" + i + ":5432/codebridge");
            shard.setRecordCount(1000 * (i + 1)); // Dummy record count
            shard.setSizeBytes(1000000 * (i + 1)); // Dummy size
            shard.setStatus("ONLINE");
            
            shards.put(i, shard);
        });
    }
}

