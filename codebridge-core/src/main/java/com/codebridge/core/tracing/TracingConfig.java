package com.codebridge.core.tracing;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for distributed tracing using Micrometer Observation API.
 */
@Configuration
public class TracingConfig {

    @Value("${codebridge.tracing.enabled:true}")
    private boolean tracingEnabled;

    /**
     * Creates an observation registry for distributed tracing.
     *
     * @return The observation registry
     */
    @Bean
    public ObservationRegistry observationRegistry() {
        ObservationRegistry registry = ObservationRegistry.create();
        
        if (tracingEnabled) {
            // Configure additional listeners or exporters here if needed
            // For example, to export traces to Zipkin, Jaeger, etc.
        }
        
        return registry;
    }

    /**
     * Creates an observed aspect for tracing method executions.
     *
     * @param observationRegistry The observation registry
     * @return The observed aspect
     */
    @Bean
    public ObservedAspect observedAspect(ObservationRegistry observationRegistry) {
        return new ObservedAspect(observationRegistry);
    }
}

