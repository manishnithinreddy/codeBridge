package com.codebridge.session.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for metrics collection and monitoring.
 * This helps prepare the system for scaling by providing visibility into performance and resource usage.
 */
@Configuration
public class MetricsConfig {

    /**
     * Configures JVM metrics for monitoring memory, GC, threads, and other JVM statistics.
     *
     * @param registry The meter registry
     * @return The configured registry with JVM metrics
     */
    @Bean
    public MeterRegistry bindJvmMetrics(MeterRegistry registry) {
        // JVM metrics
        new ClassLoaderMetrics().bindTo(registry);
        new JvmMemoryMetrics().bindTo(registry);
        new JvmGcMetrics().bindTo(registry);
        new JvmThreadMetrics().bindTo(registry);
        
        // System metrics
        new ProcessorMetrics().bindTo(registry);
        new UptimeMetrics().bindTo(registry);
        
        return registry;
    }

    /**
     * Configures the TimedAspect for @Timed annotation support.
     *
     * @param registry The meter registry
     * @return The timed aspect
     */
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
}

