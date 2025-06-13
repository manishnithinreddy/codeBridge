package com.codebridge.core.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

/**
 * Aspect for applying rate limiting to API endpoints.
 */
@Aspect
@Component
public class RateLimitAspect {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitAspect.class);

    private final ProxyManager<String> proxyManager;
    private final Bandwidth bandwidth;
    private final RateLimiterConfig rateLimiterConfig;

    @Autowired
    public RateLimitAspect(ProxyManager<String> proxyManager, Bandwidth bandwidth, RateLimiterConfig rateLimiterConfig) {
        this.proxyManager = proxyManager;
        this.bandwidth = bandwidth;
        this.rateLimiterConfig = rateLimiterConfig;
    }

    /**
     * Applies rate limiting to methods annotated with @RateLimit.
     *
     * @param joinPoint The join point
     * @param rateLimit The rate limit annotation
     * @return The result of the method execution
     * @throws Throwable If an error occurs
     */
    @Around("@annotation(rateLimit)")
    public Object rateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        
        String key = resolveKey(request, rateLimit);
        Bucket bucket = rateLimiterConfig.resolveBucket(proxyManager, bandwidth, key);
        
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        
        if (probe.isConsumed()) {
            // Add rate limit headers to the response
            addRateLimitHeaders(probe);
            return joinPoint.proceed();
        } else {
            long waitTimeMillis = probe.getNanosToWaitForRefill() / 1_000_000;
            logger.warn("Rate limit exceeded for key: {}. Need to wait {} ms", key, waitTimeMillis);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, 
                    "Rate limit exceeded. Try again in " + waitTimeMillis + " ms");
        }
    }

    /**
     * Resolves the rate limit key based on the request and annotation.
     *
     * @param request The HTTP request
     * @param rateLimit The rate limit annotation
     * @return The rate limit key
     */
    private String resolveKey(HttpServletRequest request, RateLimit rateLimit) {
        String key;
        
        switch (rateLimit.type()) {
            case IP:
                key = getClientIP(request);
                break;
            case USER:
                key = getUserIdentifier(request);
                break;
            case API:
                key = request.getRequestURI();
                break;
            default:
                key = "global";
        }
        
        return "rate-limit:" + rateLimit.name() + ":" + key;
    }

    /**
     * Gets the client IP address from the request.
     *
     * @param request The HTTP request
     * @return The client IP address
     */
    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Gets the user identifier from the request.
     *
     * @param request The HTTP request
     * @return The user identifier
     */
    private String getUserIdentifier(HttpServletRequest request) {
        // Try to get user ID from authentication
        // This is a simplified example - in a real application, you would extract the user ID from the authentication
        String userId = request.getHeader("X-User-ID");
        if (userId != null && !userId.isEmpty()) {
            return userId;
        }
        
        // Fall back to IP if user ID is not available
        return getClientIP(request);
    }

    /**
     * Adds rate limit headers to the response.
     *
     * @param probe The consumption probe
     */
    private void addRateLimitHeaders(ConsumptionProbe probe) {
        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            jakarta.servlet.http.HttpServletResponse response = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getResponse();
            
            if (response != null) {
                response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
                response.addHeader("X-Rate-Limit-Reset", String.valueOf(probe.getNanosToWaitForRefill() / 1_000_000_000));
            }
        } catch (Exception e) {
            logger.warn("Failed to add rate limit headers", e);
        }
    }
}

