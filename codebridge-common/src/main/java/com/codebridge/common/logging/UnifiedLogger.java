package com.codebridge.common.logging;

import com.codebridge.common.context.SharedContextPropagationFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Unified logger for cross-service logging.
 * Provides methods for logging messages with consistent formatting and metadata.
 */
@Component
public class UnifiedLogger {

    private static final Logger logger = LoggerFactory.getLogger(UnifiedLogger.class);
    
    @Value("${spring.application.name:unknown}")
    private String serviceName;
    
    private final LoggingEventPublisher eventPublisher;

    /**
     * Creates a new unified logger.
     *
     * @param eventPublisher The logging event publisher
     */
    @Autowired
    public UnifiedLogger(LoggingEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * Logs a message at the INFO level.
     *
     * @param message The message
     * @param metadata The metadata
     */
    public void info(String message, Map<String, Object> metadata) {
        log(LogLevel.INFO, message, metadata);
    }

    /**
     * Logs a message at the DEBUG level.
     *
     * @param message The message
     * @param metadata The metadata
     */
    public void debug(String message, Map<String, Object> metadata) {
        log(LogLevel.DEBUG, message, metadata);
    }

    /**
     * Logs a message at the WARN level.
     *
     * @param message The message
     * @param metadata The metadata
     */
    public void warn(String message, Map<String, Object> metadata) {
        log(LogLevel.WARN, message, metadata);
    }

    /**
     * Logs a message at the ERROR level.
     *
     * @param message The message
     * @param metadata The metadata
     */
    public void error(String message, Map<String, Object> metadata) {
        log(LogLevel.ERROR, message, metadata);
    }

    /**
     * Logs a message at the ERROR level with an exception.
     *
     * @param message The message
     * @param exception The exception
     * @param metadata The metadata
     */
    public void error(String message, Throwable exception, Map<String, Object> metadata) {
        log(LogLevel.ERROR, message, exception, metadata);
    }

    /**
     * Logs a message at the specified level.
     *
     * @param level The log level
     * @param message The message
     * @param metadata The metadata
     */
    public void log(LogLevel level, String message, Map<String, Object> metadata) {
        log(level, message, null, metadata);
    }

    /**
     * Logs a message at the specified level with an exception.
     *
     * @param level The log level
     * @param message The message
     * @param exception The exception
     * @param metadata The metadata
     */
    public void log(LogLevel level, String message, Throwable exception, Map<String, Object> metadata) {
        String contextId = SharedContextPropagationFilter.getCurrentContextId();
        String correlationId = contextId != null ? contextId : UUID.randomUUID().toString();
        
        try {
            // Set MDC values for the current thread
            MDC.put("correlationId", correlationId);
            MDC.put("service", serviceName);
            
            // Add metadata to MDC
            if (metadata != null) {
                metadata.forEach((key, value) -> {
                    if (value != null) {
                        MDC.put(key, value.toString());
                    }
                });
            }
            
            // Log the message
            switch (level) {
                case DEBUG:
                    if (exception != null) {
                        logger.debug(message, exception);
                    } else {
                        logger.debug(message);
                    }
                    break;
                case INFO:
                    if (exception != null) {
                        logger.info(message, exception);
                    } else {
                        logger.info(message);
                    }
                    break;
                case WARN:
                    if (exception != null) {
                        logger.warn(message, exception);
                    } else {
                        logger.warn(message);
                    }
                    break;
                case ERROR:
                    if (exception != null) {
                        logger.error(message, exception);
                    } else {
                        logger.error(message);
                    }
                    break;
            }
            
            // Publish the logging event
            LoggingEvent event = new LoggingEvent(
                    correlationId,
                    serviceName,
                    level,
                    message,
                    exception,
                    metadata
            );
            eventPublisher.publishEvent(event);
        } finally {
            // Clear MDC values
            MDC.clear();
        }
    }
}

