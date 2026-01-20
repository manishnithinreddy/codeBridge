package com.codebridge.scalability.loadbalancer;

import org.springframework.cloud.client.ServiceInstance;

import java.util.List;

/**
 * Interface for load balancing strategies.
 */
public interface LoadBalancingStrategy {

    /**
     * Selects a service instance from the list of available instances.
     *
     * @param instances the list of available service instances
     * @param serviceId the service ID
     * @param requestId the request ID (for consistent hashing)
     * @return the selected service instance
     */
    ServiceInstance selectInstance(List<ServiceInstance> instances, String serviceId, String requestId);
}

