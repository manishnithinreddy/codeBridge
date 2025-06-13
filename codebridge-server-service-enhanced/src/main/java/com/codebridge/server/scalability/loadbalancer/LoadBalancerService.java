package com.codebridge.server.scalability.loadbalancer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for load balancing requests across multiple server instances
 */
@Service
@Slf4j
public class LoadBalancerService {

    private final String strategy;
    private final Map<String, String> stickySessionMap = new ConcurrentHashMap<>();
    private final AtomicInteger roundRobinCounter = new AtomicInteger(0);
    
    @Value("${codebridge.scalability.load-balancing.sticky-sessions:true}")
    private boolean stickySessions;
    
    @Value("${codebridge.scalability.load-balancing.health-check-interval-seconds:30}")
    private int healthCheckIntervalSeconds;

    public LoadBalancerService(String strategy) {
        this.strategy = strategy;
        log.info("Load balancer initialized with strategy: {}", strategy);
    }

    /**
     * Select a server instance based on the configured load balancing strategy
     *
     * @param availableServers List of available server instances
     * @param sessionId Optional session ID for sticky sessions
     * @return Selected server instance
     */
    public String selectServer(List<String> availableServers, String sessionId) {
        if (availableServers == null || availableServers.isEmpty()) {
            throw new IllegalArgumentException("No available servers to select from");
        }
        
        // If sticky sessions are enabled and we have a session ID, try to use the same server
        if (stickySessions && sessionId != null && !sessionId.isEmpty()) {
            String existingServer = stickySessionMap.get(sessionId);
            if (existingServer != null && availableServers.contains(existingServer)) {
                log.debug("Using sticky session for session ID: {}, server: {}", sessionId, existingServer);
                return existingServer;
            }
        }
        
        // Select a server based on the strategy
        String selectedServer;
        switch (strategy.toLowerCase()) {
            case "round-robin":
                selectedServer = roundRobinStrategy(availableServers);
                break;
            case "least-connections":
                selectedServer = leastConnectionsStrategy(availableServers);
                break;
            case "ip-hash":
                selectedServer = ipHashStrategy(availableServers, sessionId);
                break;
            case "weighted":
                selectedServer = weightedStrategy(availableServers);
                break;
            default:
                selectedServer = randomStrategy(availableServers);
                break;
        }
        
        // Store the selection for sticky sessions
        if (stickySessions && sessionId != null && !sessionId.isEmpty()) {
            stickySessionMap.put(sessionId, selectedServer);
        }
        
        return selectedServer;
    }

    /**
     * Round-robin load balancing strategy
     */
    private String roundRobinStrategy(List<String> availableServers) {
        int index = roundRobinCounter.getAndIncrement() % availableServers.size();
        if (roundRobinCounter.get() > 10000) {
            roundRobinCounter.set(0); // Reset to avoid overflow
        }
        return availableServers.get(index);
    }

    /**
     * Random load balancing strategy
     */
    private String randomStrategy(List<String> availableServers) {
        int index = (int) (Math.random() * availableServers.size());
        return availableServers.get(index);
    }

    /**
     * IP hash load balancing strategy
     */
    private String ipHashStrategy(List<String> availableServers, String clientIdentifier) {
        if (clientIdentifier == null || clientIdentifier.isEmpty()) {
            return randomStrategy(availableServers);
        }
        
        int hash = clientIdentifier.hashCode();
        int index = Math.abs(hash % availableServers.size());
        return availableServers.get(index);
    }

    /**
     * Least connections load balancing strategy
     * Note: This is a simplified implementation. In a real system, you would track actual connection counts.
     */
    private String leastConnectionsStrategy(List<String> availableServers) {
        // In a real implementation, we would track connection counts per server
        // For this example, we'll just use round-robin as a placeholder
        return roundRobinStrategy(availableServers);
    }

    /**
     * Weighted load balancing strategy
     * Note: This is a simplified implementation. In a real system, you would have actual weights.
     */
    private String weightedStrategy(List<String> availableServers) {
        // In a real implementation, we would use server weights
        // For this example, we'll just use round-robin as a placeholder
        return roundRobinStrategy(availableServers);
    }

    /**
     * Remove a session from the sticky session map
     */
    public void removeSession(String sessionId) {
        if (sessionId != null && !sessionId.isEmpty()) {
            stickySessionMap.remove(sessionId);
        }
    }

    /**
     * Clear all sticky sessions
     */
    public void clearSessions() {
        stickySessionMap.clear();
    }
}

