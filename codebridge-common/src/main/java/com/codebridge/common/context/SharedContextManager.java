package com.codebridge.common.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager for shared contexts.
 * Provides methods for creating, retrieving, and managing shared contexts.
 */
@Component
public class SharedContextManager {

    private static final Logger logger = LoggerFactory.getLogger(SharedContextManager.class);
    private final Map<String, SharedContext> contexts = new ConcurrentHashMap<>();

    /**
     * Creates a new shared context.
     *
     * @return The shared context
     */
    public SharedContext createContext() {
        SharedContext context = new SharedContext();
        contexts.put(context.getId(), context);
        logger.debug("Created shared context: {}", context.getId());
        return context;
    }

    /**
     * Gets a shared context by ID.
     *
     * @param id The context ID
     * @return The shared context, or null if not found
     */
    public SharedContext getContext(String id) {
        SharedContext context = contexts.get(id);
        if (context != null && context.isExpired()) {
            removeContext(id);
            logger.debug("Removed expired shared context: {}", id);
            return null;
        }
        return context;
    }

    /**
     * Removes a shared context by ID.
     *
     * @param id The context ID
     * @return The removed shared context, or null if not found
     */
    public SharedContext removeContext(String id) {
        return contexts.remove(id);
    }

    /**
     * Updates the expiration time for a shared context.
     *
     * @param id The context ID
     * @param expiresAt The new expiration time
     * @return True if the context was updated, false otherwise
     */
    public boolean updateContextExpiration(String id, LocalDateTime expiresAt) {
        SharedContext context = getContext(id);
        if (context != null) {
            context.setExpiresAt(expiresAt);
            return true;
        }
        return false;
    }

    /**
     * Extends the expiration time for a shared context by a specified number of minutes.
     *
     * @param id The context ID
     * @param minutes The number of minutes to extend the expiration by
     * @return True if the context was updated, false otherwise
     */
    public boolean extendContextExpiration(String id, long minutes) {
        SharedContext context = getContext(id);
        if (context != null) {
            context.setExpiresAt(context.getExpiresAt().plusMinutes(minutes));
            return true;
        }
        return false;
    }

    /**
     * Gets the number of active contexts.
     *
     * @return The number of active contexts
     */
    public int getActiveContextCount() {
        return contexts.size();
    }

    /**
     * Cleans up expired contexts.
     * This method is scheduled to run every 15 minutes.
     */
    @Scheduled(fixedRate = 900000) // 15 minutes
    public void cleanupExpiredContexts() {
        int expiredCount = 0;
        for (Map.Entry<String, SharedContext> entry : contexts.entrySet()) {
            if (entry.getValue().isExpired()) {
                contexts.remove(entry.getKey());
                expiredCount++;
            }
        }
        if (expiredCount > 0) {
            logger.info("Cleaned up {} expired shared contexts", expiredCount);
        }
    }
}

