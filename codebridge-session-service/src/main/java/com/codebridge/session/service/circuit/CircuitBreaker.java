package com.codebridge.session.service.circuit;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * A simple circuit breaker implementation to prevent cascading failures.
 * This helps improve system resilience by failing fast when a service is experiencing problems.
 *
 * @param <T> The type of result returned by the protected operation
 */
public class CircuitBreaker<T> {
    private static final Logger logger = LoggerFactory.getLogger(CircuitBreaker.class);

    private final String name;
    private final int failureThreshold;
    private final Duration resetTimeout;
    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicReference<Instant> lastStateChangeTime = new AtomicReference<>(Instant.now());
    
    // Metrics
    private final Counter successCounter;
    private final Counter failureCounter;
    private final Counter openCounter;
    private final Counter halfOpenCounter;
    private final Counter closedCounter;

    /**
     * Creates a new circuit breaker.
     *
     * @param name The name of this circuit breaker (used for logging and metrics)
     * @param failureThreshold The number of consecutive failures that will trip the circuit
     * @param resetTimeout The duration after which to try resetting the circuit
     * @param meterRegistry The meter registry for recording metrics
     */
    public CircuitBreaker(String name, int failureThreshold, Duration resetTimeout, MeterRegistry meterRegistry) {
        this.name = name;
        this.failureThreshold = failureThreshold;
        this.resetTimeout = resetTimeout;
        
        // Initialize metrics
        this.successCounter = Counter.builder("circuit.breaker." + name + ".success")
                .description("Number of successful executions")
                .register(meterRegistry);
        
        this.failureCounter = Counter.builder("circuit.breaker." + name + ".failure")
                .description("Number of failed executions")
                .register(meterRegistry);
        
        this.openCounter = Counter.builder("circuit.breaker." + name + ".state.open")
                .description("Number of times circuit entered open state")
                .register(meterRegistry);
        
        this.halfOpenCounter = Counter.builder("circuit.breaker." + name + ".state.half.open")
                .description("Number of times circuit entered half-open state")
                .register(meterRegistry);
        
        this.closedCounter = Counter.builder("circuit.breaker." + name + ".state.closed")
                .description("Number of times circuit entered closed state")
                .register(meterRegistry);
        
        // Initialize state
        closedCounter.increment();
    }

    /**
     * Executes the given operation with circuit breaker protection.
     *
     * @param operation The operation to execute
     * @return The result of the operation
     * @throws CircuitBreakerOpenException If the circuit is open
     * @throws Exception If the operation throws an exception
     */
    public T execute(Supplier<T> operation) throws Exception {
        if (isOpen()) {
            // Check if it's time to try resetting
            if (Duration.between(lastStateChangeTime.get(), Instant.now()).compareTo(resetTimeout) > 0) {
                setState(State.HALF_OPEN);
            } else {
                throw new CircuitBreakerOpenException("Circuit breaker '" + name + "' is open");
            }
        }
        
        try {
            T result = operation.get();
            recordSuccess();
            return result;
        } catch (Exception e) {
            recordFailure();
            throw e;
        }
    }

    /**
     * Records a successful execution.
     */
    private void recordSuccess() {
        successCounter.increment();
        if (state.get() == State.HALF_OPEN) {
            setState(State.CLOSED);
        }
        failureCount.set(0);
    }

    /**
     * Records a failed execution.
     */
    private void recordFailure() {
        failureCounter.increment();
        if (state.get() == State.CLOSED) {
            if (failureCount.incrementAndGet() >= failureThreshold) {
                setState(State.OPEN);
            }
        } else if (state.get() == State.HALF_OPEN) {
            setState(State.OPEN);
        }
    }

    /**
     * Checks if the circuit is currently open.
     *
     * @return true if the circuit is open, false otherwise
     */
    public boolean isOpen() {
        return state.get() == State.OPEN;
    }

    /**
     * Gets the current state of the circuit.
     *
     * @return The current state
     */
    public State getState() {
        return state.get();
    }

    /**
     * Sets the state of the circuit.
     *
     * @param newState The new state
     */
    private void setState(State newState) {
        State oldState = state.getAndSet(newState);
        if (oldState != newState) {
            lastStateChangeTime.set(Instant.now());
            logger.info("Circuit breaker '{}' state changed from {} to {}", name, oldState, newState);
            
            // Update metrics
            switch (newState) {
                case OPEN:
                    openCounter.increment();
                    break;
                case HALF_OPEN:
                    halfOpenCounter.increment();
                    break;
                case CLOSED:
                    closedCounter.increment();
                    break;
            }
        }
    }

    /**
     * Resets the circuit breaker to its initial closed state.
     */
    public void reset() {
        setState(State.CLOSED);
        failureCount.set(0);
    }

    /**
     * The possible states of a circuit breaker.
     */
    public enum State {
        /**
         * Circuit is closed and operations are allowed to execute.
         */
        CLOSED,
        
        /**
         * Circuit is open and operations will fail fast.
         */
        OPEN,
        
        /**
         * Circuit is allowing a single operation to test if the system has recovered.
         */
        HALF_OPEN
    }

    /**
     * Exception thrown when an operation is attempted while the circuit is open.
     */
    public static class CircuitBreakerOpenException extends RuntimeException {
        public CircuitBreakerOpenException(String message) {
            super(message);
        }
    }
}

