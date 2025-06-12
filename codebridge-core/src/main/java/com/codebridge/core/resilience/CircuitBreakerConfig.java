package com.codebridge.core.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuration for circuit breakers using Resilience4j.
 */
@Configuration
public class CircuitBreakerConfig {

    private static final Logger logger = LoggerFactory.getLogger(CircuitBreakerConfig.class);

    @Value("${codebridge.resilience.circuitbreaker.failure-rate-threshold:50}")
    private float failureRateThreshold;

    @Value("${codebridge.resilience.circuitbreaker.slow-call-rate-threshold:50}")
    private float slowCallRateThreshold;

    @Value("${codebridge.resilience.circuitbreaker.slow-call-duration-threshold:2000}")
    private long slowCallDurationThreshold;

    @Value("${codebridge.resilience.circuitbreaker.permitted-calls-in-half-open-state:10}")
    private int permittedCallsInHalfOpenState;

    @Value("${codebridge.resilience.circuitbreaker.sliding-window-size:100}")
    private int slidingWindowSize;

    @Value("${codebridge.resilience.circuitbreaker.minimum-number-of-calls:10}")
    private int minimumNumberOfCalls;

    @Value("${codebridge.resilience.circuitbreaker.wait-duration-in-open-state:60000}")
    private long waitDurationInOpenState;

    /**
     * Creates a circuit breaker registry with default configuration.
     *
     * @return The circuit breaker registry
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        io.github.resilience4j.circuitbreaker.CircuitBreakerConfig config = io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                .failureRateThreshold(failureRateThreshold)
                .slowCallRateThreshold(slowCallRateThreshold)
                .slowCallDurationThreshold(Duration.ofMillis(slowCallDurationThreshold))
                .permittedNumberOfCallsInHalfOpenState(permittedCallsInHalfOpenState)
                .slidingWindowType(SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(slidingWindowSize)
                .minimumNumberOfCalls(minimumNumberOfCalls)
                .waitDurationInOpenState(Duration.ofMillis(waitDurationInOpenState))
                .build();

        return CircuitBreakerRegistry.of(config);
    }

    /**
     * Creates a registry event consumer for logging circuit breaker events.
     *
     * @return The registry event consumer
     */
    @Bean
    public RegistryEventConsumer<CircuitBreaker> circuitBreakerEventConsumer() {
        return new RegistryEventConsumer<CircuitBreaker>() {
            @Override
            public void onEntryAddedEvent(EntryAddedEvent<CircuitBreaker> entryAddedEvent) {
                CircuitBreaker circuitBreaker = entryAddedEvent.getAddedEntry();
                logger.info("Circuit breaker {} added", circuitBreaker.getName());
                
                // Register event handlers
                circuitBreaker.getEventPublisher()
                        .onStateTransition(event -> logger.info("Circuit breaker {} state changed from {} to {}", 
                                event.getCircuitBreakerName(), event.getStateTransition().getFromState(), 
                                event.getStateTransition().getToState()))
                        .onError(event -> logger.debug("Circuit breaker {} recorded error: {}", 
                                event.getCircuitBreakerName(), event.getThrowable().getMessage()))
                        .onSuccess(event -> logger.debug("Circuit breaker {} recorded success", 
                                event.getCircuitBreakerName()));
            }

            @Override
            public void onEntryRemovedEvent(EntryRemovedEvent<CircuitBreaker> entryRemoveEvent) {
                logger.info("Circuit breaker {} removed", entryRemoveEvent.getRemovedEntry().getName());
            }

            @Override
            public void onEntryReplacedEvent(EntryReplacedEvent<CircuitBreaker> entryReplacedEvent) {
                logger.info("Circuit breaker {} replaced", entryReplacedEvent.getNewEntry().getName());
            }
        };
    }
}

