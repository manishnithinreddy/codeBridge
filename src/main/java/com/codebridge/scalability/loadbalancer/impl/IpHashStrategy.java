package com.codebridge.scalability.loadbalancer.impl;

import com.codebridge.scalability.loadbalancer.LoadBalancingStrategy;
import org.springframework.cloud.client.ServiceInstance;

import java.util.List;

/**
 * IP hash load balancing strategy.
 * Consistently routes requests from the same client to the same instance.
 */
public class IpHashStrategy implements LoadBalancingStrategy {

    @Override
    public ServiceInstance selectInstance(List<ServiceInstance> instances, String serviceId, String requestId) {
        if (instances == null || instances.isEmpty()) {
            return null;
        }
        
        // If requestId is null or empty, fall back to round-robin
        if (requestId == null || requestId.isEmpty()) {
            return new RoundRobinStrategy().selectInstance(instances, serviceId, requestId);
        }
        
        // Use consistent hashing to select an instance
        int hash = requestId.hashCode();
        int index = Math.abs(hash % instances.size());
        
        return instances.get(index);
    }
}

