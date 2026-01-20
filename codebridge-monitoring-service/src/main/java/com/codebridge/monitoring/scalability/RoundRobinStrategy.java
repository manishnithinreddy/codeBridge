package com.codebridge.monitoring.scalability.loadbalancer.impl;

import com.codebridge.monitoring.scalability.loadbalancer.LoadBalancingStrategy;
import org.springframework.cloud.client.ServiceInstance;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Round-robin load balancing strategy.
 * Distributes requests evenly across all instances in a circular order.
 */
public class RoundRobinStrategy implements LoadBalancingStrategy {

    private final Map<String, AtomicInteger> counterMap = new ConcurrentHashMap<>();

    @Override
    public ServiceInstance selectInstance(List<ServiceInstance> instances, String serviceId, String requestId) {
        if (instances == null || instances.isEmpty()) {
            return null;
        }
        
        AtomicInteger counter = counterMap.computeIfAbsent(serviceId, k -> new AtomicInteger(0));
        int index = counter.getAndIncrement() % instances.size();
        
        // Handle integer overflow
        if (counter.get() > 10_000_000) {
            counter.set(0);
        }
        
        return instances.get(index);
    }
}

