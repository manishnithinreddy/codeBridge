package com.codebridge.scalability.loadbalancer;

import org.springframework.cloud.client.ServiceInstance;

/**
 * Interface for health checking service instances.
 */
public interface HealthCheckService {

    /**
     * Checks if a service instance is healthy.
     *
     * @param instance the service instance to check
     * @return true if the instance is healthy, false otherwise
     */
    boolean isHealthy(ServiceInstance instance);

    /**
     * Starts health checking for a service instance.
     *
     * @param instance the service instance to check
     * @param serviceId the service ID
     */
    void startHealthCheck(ServiceInstance instance, String serviceId);

    /**
     * Stops health checking for a service instance.
     *
     * @param instance the service instance to stop checking
     */
    void stopHealthCheck(ServiceInstance instance);
}

