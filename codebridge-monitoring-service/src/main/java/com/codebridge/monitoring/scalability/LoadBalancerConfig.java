package com.codebridge.monitoring.scalability.config;

import com.codebridge.monitoring.scalability.loadbalancer.HealthCheckService;
import com.codebridge.monitoring.scalability.loadbalancer.LoadBalancingStrategy;
import com.codebridge.monitoring.scalability.loadbalancer.ServiceInstanceSelector;
import com.codebridge.monitoring.scalability.loadbalancer.impl.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for load balancing.
 */
@Configuration
@RequiredArgsConstructor
public class LoadBalancerConfig {

    private final DiscoveryClient discoveryClient;

    @Value("${codebridge.scalability.load-balancing.strategy}")
    private String loadBalancingStrategy;

    @Value("${codebridge.scalability.load-balancing.sticky-sessions}")
    private boolean stickySessionsEnabled;

    @Value("${codebridge.scalability.load-balancing.health-check-interval-seconds}")
    private int healthCheckIntervalSeconds;

    /**
     * Creates a load-balanced RestTemplate.
     *
     * @return the RestTemplate
     */
    @Bean
    @LoadBalanced
    public RestTemplate loadBalancedRestTemplate() {
        return new RestTemplate();
    }

    /**
     * Creates a health check service.
     *
     * @return the health check service
     */
    @Bean
    public HealthCheckService healthCheckService() {
        return new DefaultHealthCheckService(loadBalancedRestTemplate(), healthCheckIntervalSeconds);
    }

    /**
     * Creates a load balancing strategy based on the configuration.
     *
     * @return the load balancing strategy
     */
    @Bean
    public LoadBalancingStrategy loadBalancingStrategy() {
        switch (loadBalancingStrategy.toLowerCase()) {
            case "round-robin":
                return new RoundRobinStrategy();
            case "least-connections":
                return new LeastConnectionsStrategy();
            case "weighted":
                return new WeightedStrategy();
            case "ip-hash":
                return new IpHashStrategy();
            default:
                return new RoundRobinStrategy();
        }
    }

    /**
     * Creates a service instance selector.
     *
     * @return the service instance selector
     */
    @Bean
    public ServiceInstanceSelector serviceInstanceSelector() {
        ServiceInstanceSelector selector = new DefaultServiceInstanceSelector(
                discoveryClient, loadBalancingStrategy(), healthCheckService());
        
        if (stickySessionsEnabled) {
            return new StickySessionServiceInstanceSelector(selector);
        }
        
        return selector;
    }
}

