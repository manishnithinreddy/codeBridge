package com.codebridge.monitoring.scalability.resilience.impl;

import com.codebridge.monitoring.scalability.resilience.ReplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Default implementation of the replication service.
 */
@Slf4j
public class DefaultReplicationService implements ReplicationService {

    private final boolean enabled;
    private final boolean readFromReplicas;
    private String consistencyLevel;
    private final JdbcTemplate jdbcTemplate;
    
    private final List<String> replicas = new ArrayList<>();
    private final ConcurrentHashMap<String, Boolean> replicaHealth = new ConcurrentHashMap<>();
    private final AtomicInteger roundRobinCounter = new AtomicInteger(0);
    
    private static final List<String> VALID_CONSISTENCY_LEVELS = Arrays.asList(
            "ONE", "QUORUM", "ALL"
    );

    /**
     * Creates a new DefaultReplicationService.
     *
     * @param enabled whether replication is enabled
     * @param readFromReplicas whether to read from replicas
     * @param consistencyLevel the consistency level
     * @param jdbcTemplate the JDBC template
     */
    public DefaultReplicationService(boolean enabled, boolean readFromReplicas, 
                                    String consistencyLevel, JdbcTemplate jdbcTemplate) {
        this.enabled = enabled;
        this.readFromReplicas = readFromReplicas;
        this.consistencyLevel = validateConsistencyLevel(consistencyLevel);
        this.jdbcTemplate = jdbcTemplate;
        
        // Initialize with some dummy replicas for demonstration
        if (enabled) {
            replicas.add("replica1");
            replicas.add("replica2");
            replicas.add("replica3");
            
            // Mark all replicas as healthy initially
            replicas.forEach(replica -> replicaHealth.put(replica, true));
        }
    }

    @Override
    public boolean replicateData(String sql, Object... params) {
        if (!enabled) {
            return true;
        }
        
        try {
            // Execute on primary
            jdbcTemplate.update(sql, params);
            
            // Replicate to replicas based on consistency level
            int successCount = 1; // Primary already succeeded
            List<String> healthyReplicas = getHealthyReplicas();
            
            for (String replica : healthyReplicas) {
                try {
                    // In a real implementation, this would use a connection to the replica
                    // For demonstration, we'll just log it
                    log.info("Replicating to {}: {}", replica, sql);
                    successCount++;
                    
                    // If consistency level is ONE, we can stop after the first successful replica
                    if (consistencyLevel.equals("ONE") && successCount > 1) {
                        break;
                    }
                } catch (Exception e) {
                    log.error("Failed to replicate to {}: {}", replica, e.getMessage());
                    markReplicaUnhealthy(replica);
                    
                    // If consistency level is ALL, we need all replicas to succeed
                    if (consistencyLevel.equals("ALL")) {
                        return false;
                    }
                }
            }
            
            // For QUORUM, we need a majority of replicas to succeed
            if (consistencyLevel.equals("QUORUM")) {
                int totalNodes = 1 + healthyReplicas.size(); // Primary + replicas
                int quorum = (totalNodes / 2) + 1;
                
                if (successCount < quorum) {
                    log.error("Failed to achieve quorum: {} out of {} nodes succeeded", successCount, totalNodes);
                    return false;
                }
            }
            
            return true;
        } catch (Exception e) {
            log.error("Failed to replicate data: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getReplicaForRead() {
        if (!enabled || !readFromReplicas) {
            return null; // Use primary
        }
        
        List<String> healthyReplicas = getHealthyReplicas();
        
        if (healthyReplicas.isEmpty()) {
            return null; // No healthy replicas, use primary
        }
        
        // Round-robin selection
        int index = roundRobinCounter.getAndIncrement() % healthyReplicas.size();
        
        // Handle integer overflow
        if (roundRobinCounter.get() > 10_000_000) {
            roundRobinCounter.set(0);
        }
        
        return healthyReplicas.get(index);
    }

    @Override
    public List<String> getAvailableReplicas() {
        return new ArrayList<>(replicas);
    }

    @Override
    public boolean isReplicaHealthy(String replicaName) {
        Boolean healthy = replicaHealth.get(replicaName);
        return healthy != null && healthy;
    }

    @Override
    public String getConsistencyLevel() {
        return consistencyLevel;
    }

    @Override
    public void setConsistencyLevel(String consistencyLevel) {
        this.consistencyLevel = validateConsistencyLevel(consistencyLevel);
    }
    
    private List<String> getHealthyReplicas() {
        List<String> healthyReplicas = new ArrayList<>();
        
        for (String replica : replicas) {
            if (isReplicaHealthy(replica)) {
                healthyReplicas.add(replica);
            }
        }
        
        return healthyReplicas;
    }
    
    private void markReplicaUnhealthy(String replica) {
        replicaHealth.put(replica, false);
        
        // In a real implementation, we would start a health check to see when it comes back
        // For demonstration, we'll just randomly mark it as healthy again after some time
        new Thread(() -> {
            try {
                Thread.sleep(new Random().nextInt(10000) + 5000);
                replicaHealth.put(replica, true);
                log.info("Replica {} is healthy again", replica);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
    private String validateConsistencyLevel(String level) {
        if (level == null || !VALID_CONSISTENCY_LEVELS.contains(level.toUpperCase())) {
            log.warn("Invalid consistency level: {}. Using QUORUM instead.", level);
            return "QUORUM";
        }
        
        return level.toUpperCase();
    }
}

