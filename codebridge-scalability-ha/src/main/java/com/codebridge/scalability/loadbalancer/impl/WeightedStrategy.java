package com.codebridge.scalability.loadbalancer.impl;

import com.codebridge.scalability.loadbalancer.LoadBalancingStrategy;
import org.springframework.cloud.client.ServiceInstance;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Weighted load balancing strategy.
 * Distributes requests based on instance weights.
 */
public class WeightedStrategy implements LoadBalancingStrategy {

    private static final String WEIGHT_METADATA_KEY = "weight";
    private static final int DEFAULT_WEIGHT = 100;
    private final Random random = new Random();

    @Override
    public ServiceInstance selectInstance(List<ServiceInstance> instances, String serviceId, String requestId) {
        if (instances == null || instances.isEmpty()) {
            return null;
        }
        
        // Calculate total weight
        int totalWeight = instances.stream()
                .mapToInt(this::getInstanceWeight)
                .sum();
        
        // Select an instance based on weight
        int randomWeight = random.nextInt(totalWeight);
        int currentWeight = 0;
        
        for (ServiceInstance instance : instances) {
            currentWeight += getInstanceWeight(instance);
            
            if (randomWeight < currentWeight) {
                return instance;
            }
        }
        
        // Fallback to the first instance
        return instances.get(0);
    }
    
    private int getInstanceWeight(ServiceInstance instance) {
        Map<String, String> metadata = instance.getMetadata();
        
        if (metadata != null && metadata.containsKey(WEIGHT_METADATA_KEY)) {
            try {
                return Integer.parseInt(metadata.get(WEIGHT_METADATA_KEY));
            } catch (NumberFormatException e) {
                return DEFAULT_WEIGHT;
            }
        }
        
        return DEFAULT_WEIGHT;
    }
}

