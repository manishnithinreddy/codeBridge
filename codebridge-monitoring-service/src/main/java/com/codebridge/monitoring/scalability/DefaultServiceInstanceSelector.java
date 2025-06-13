package com.codebridge.monitoring.scalability.loadbalancer.impl;

import com.codebridge.monitoring.scalability.loadbalancer.HealthCheckService;
import com.codebridge.monitoring.scalability.loadbalancer.LoadBalancingStrategy;
import com.codebridge.monitoring.scalability.loadbalancer.ServiceInstanceSelector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Default implementation of the service instance selector.
 * Selects instances based on the configured load balancing strategy and health status.
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultServiceInstanceSelector implements ServiceInstanceSelector {

    private final DiscoveryClient discoveryClient;
    private final LoadBalancingStrategy loadBalancingStrategy;
    private final HealthCheckService healthCheckService;
    
    private final Set<String> unhealthyInstances = ConcurrentHashMap.newKeySet();

    @Override
    public Optional<ServiceInstance> selectInstance(String serviceId, String requestId) {
        List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);
        
        if (instances.isEmpty()) {
            log.warn("No instances found for service: {}", serviceId);
            return Optional.empty();
        }
        
        // Filter out unhealthy instances
        List<ServiceInstance> healthyInstances = instances.stream()
                .filter(instance -> isHealthy(serviceId, instance))
                .collect(Collectors.toList());
        
        if (healthyInstances.isEmpty()) {
            log.warn("No healthy instances found for service: {}", serviceId);
            return Optional.empty();
        }
        
        // Start health checks for all instances
        instances.forEach(instance -> healthCheckService.startHealthCheck(instance, serviceId));
        
        // Select an instance using the load balancing strategy
        ServiceInstance selected = loadBalancingStrategy.selectInstance(healthyInstances, serviceId, requestId);
        
        return Optional.ofNullable(selected);
    }

    @Override
    public void markInstanceUnhealthy(String serviceId, String instanceId) {
        String key = getInstanceKey(serviceId, instanceId);
        unhealthyInstances.add(key);
        log.info("Marked instance as unhealthy: {}", key);
    }

    @Override
    public void markInstanceHealthy(String serviceId, String instanceId) {
        String key = getInstanceKey(serviceId, instanceId);
        unhealthyInstances.remove(key);
        log.info("Marked instance as healthy: {}", key);
    }
    
    private boolean isHealthy(String serviceId, ServiceInstance instance) {
        String instanceId = instance.getInstanceId();
        String key = getInstanceKey(serviceId, instanceId);
        
        if (unhealthyInstances.contains(key)) {
            return false;
        }
        
        return healthCheckService.isHealthy(instance);
    }
    
    private String getInstanceKey(String serviceId, String instanceId) {
        return serviceId + ":" + instanceId;
    }
}

