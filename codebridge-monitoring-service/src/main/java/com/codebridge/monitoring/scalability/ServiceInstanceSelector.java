package com.codebridge.monitoring.scalability.loadbalancer;

import org.springframework.cloud.client.ServiceInstance;

import java.util.Optional;

/**
 * Interface for selecting service instances based on load balancing strategies.
 */
public interface ServiceInstanceSelector {

    /**
     * Selects a service instance for the given service ID.
     *
     * @param serviceId the service ID
     * @param requestId the request ID (for consistent hashing)
     * @return an optional containing the selected service instance, or empty if none available
     */
    Optional<ServiceInstance> selectInstance(String serviceId, String requestId);

    /**
     * Marks a service instance as unhealthy.
     *
     * @param serviceId the service ID
     * @param instanceId the instance ID
     */
    void markInstanceUnhealthy(String serviceId, String instanceId);

    /**
     * Marks a service instance as healthy.
     *
     * @param serviceId the service ID
     * @param instanceId the instance ID
     */
    void markInstanceHealthy(String serviceId, String instanceId);
}

