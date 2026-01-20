package com.codebridge.monitoring.scalability.loadbalancer.impl;

import com.codebridge.monitoring.scalability.loadbalancer.LoadBalancingStrategy;
import org.springframework.cloud.client.ServiceInstance;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Least connections load balancing strategy.
 * Selects the instance with the fewest active connections.
 */
public class LeastConnectionsStrategy implements LoadBalancingStrategy {

    private final Map<String, AtomicInteger> connectionCounts = new ConcurrentHashMap<>();

    @Override
    public ServiceInstance selectInstance(List<ServiceInstance> instances, String serviceId, String requestId) {
        if (instances == null || instances.isEmpty()) {
            return null;
        }
        
        // Find the instance with the least connections
        ServiceInstance selected = instances.stream()
                .min(Comparator.comparingInt(instance -> 
                        connectionCounts.getOrDefault(getInstanceKey(serviceId, instance), new AtomicInteger(0)).get()))
                .orElse(instances.get(0));
        
        // Increment the connection count for the selected instance
        String instanceKey = getInstanceKey(serviceId, selected);
        connectionCounts.computeIfAbsent(instanceKey, k -> new AtomicInteger(0)).incrementAndGet();
        
        return selected;
    }
    
    /**
     * Decrements the connection count for an instance when a request completes.
     *
     * @param serviceId the service ID
     * @param instance the service instance
     */
    public void decrementConnectionCount(String serviceId, ServiceInstance instance) {
        String instanceKey = getInstanceKey(serviceId, instance);
        AtomicInteger count = connectionCounts.get(instanceKey);
        
        if (count != null && count.get() > 0) {
            count.decrementAndGet();
        }
    }
    
    private String getInstanceKey(String serviceId, ServiceInstance instance) {
        return serviceId + ":" + instance.getInstanceId();
    }
}

