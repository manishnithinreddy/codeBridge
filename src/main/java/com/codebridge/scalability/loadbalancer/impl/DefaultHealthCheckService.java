package com.codebridge.scalability.loadbalancer.impl;

import com.codebridge.scalability.loadbalancer.HealthCheckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of the health check service.
 * Periodically checks the health of service instances.
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultHealthCheckService implements HealthCheckService {

    private static final String HEALTH_CHECK_PATH = "/actuator/health";
    
    private final RestTemplate restTemplate;
    private final int healthCheckIntervalSeconds;
    
    private final Map<String, ServiceInstance> instancesUnderCheck = new ConcurrentHashMap<>();
    private final Set<String> unhealthyInstances = ConcurrentHashMap.newKeySet();
    private final Map<String, String> instanceToServiceMap = new ConcurrentHashMap<>();

    @Override
    public boolean isHealthy(ServiceInstance instance) {
        String instanceId = getInstanceId(instance);
        return !unhealthyInstances.contains(instanceId);
    }

    @Override
    public void startHealthCheck(ServiceInstance instance, String serviceId) {
        String instanceId = getInstanceId(instance);
        instancesUnderCheck.put(instanceId, instance);
        instanceToServiceMap.put(instanceId, serviceId);
    }

    @Override
    public void stopHealthCheck(ServiceInstance instance) {
        String instanceId = getInstanceId(instance);
        instancesUnderCheck.remove(instanceId);
        instanceToServiceMap.remove(instanceId);
        unhealthyInstances.remove(instanceId);
    }

    /**
     * Periodically checks the health of all instances under check.
     */
    @Scheduled(fixedDelayString = "${codebridge.scalability.load-balancing.health-check-interval-seconds:30}000")
    public void checkHealth() {
        instancesUnderCheck.forEach((instanceId, instance) -> {
            try {
                String healthUrl = instance.getUri() + HEALTH_CHECK_PATH;
                ResponseEntity<Map> response = restTemplate.getForEntity(healthUrl, Map.class);
                
                if (response.getStatusCode() == HttpStatus.OK) {
                    Map<String, Object> body = response.getBody();
                    String status = body != null ? (String) body.get("status") : null;
                    
                    if ("UP".equals(status)) {
                        if (unhealthyInstances.remove(instanceId)) {
                            log.info("Instance {} is now healthy", instanceId);
                        }
                    } else {
                        markUnhealthy(instanceId);
                    }
                } else {
                    markUnhealthy(instanceId);
                }
            } catch (Exception e) {
                log.warn("Health check failed for instance {}: {}", instanceId, e.getMessage());
                markUnhealthy(instanceId);
            }
        });
    }
    
    private void markUnhealthy(String instanceId) {
        if (unhealthyInstances.add(instanceId)) {
            log.warn("Instance {} is unhealthy", instanceId);
        }
    }
    
    private String getInstanceId(ServiceInstance instance) {
        return instance.getServiceId() + ":" + instance.getInstanceId();
    }
}

