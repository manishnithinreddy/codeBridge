package com.codebridge.core.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.function.Supplier;

/**
 * Custom aspect for applying circuit breakers to methods annotated with @CircuitBreaker.
 * This is separate from Resilience4j's auto-configured CircuitBreakerAspect.
 */
@Aspect
@Component("customCircuitBreakerAspect")
public class CustomCircuitBreakerAspect {

    private static final Logger logger = LoggerFactory.getLogger(CustomCircuitBreakerAspect.class);

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    public CustomCircuitBreakerAspect(CircuitBreakerRegistry circuitBreakerRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    /**
     * Applies a circuit breaker to methods annotated with @CircuitBreaker.
     *
     * @param joinPoint The join point
     * @param circuitBreakerAnnotation The circuit breaker annotation
     * @return The result of the method execution
     * @throws Throwable If an error occurs
     */
    @Around("@annotation(circuitBreakerAnnotation)")
    public Object circuitBreaker(ProceedingJoinPoint joinPoint, com.codebridge.core.resilience.CircuitBreaker circuitBreakerAnnotation) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        String circuitBreakerName = circuitBreakerAnnotation.name().isEmpty() 
                ? method.getDeclaringClass().getSimpleName() + "#" + method.getName() 
                : circuitBreakerAnnotation.name();
        
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(circuitBreakerName);
        
        Supplier<Object> supplier = () -> {
            try {
                return joinPoint.proceed();
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        };
        
        return circuitBreaker.executeSupplier(supplier);
    }
}
