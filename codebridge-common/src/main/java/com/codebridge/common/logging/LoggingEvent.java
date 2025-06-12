package com.codebridge.common.logging;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Event for logging messages.
 * Contains information about a log message.
 */
public class LoggingEvent {

    private final String correlationId;
    private final String service;
    private final LogLevel level;
    private final String message;
    private final Throwable exception;
    private final Map<String, Object> metadata;
    private final LocalDateTime timestamp;

    /**
     * Creates a new logging event.
     *
     * @param correlationId The correlation ID
     * @param service The service name
     * @param level The log level
     * @param message The message
     * @param exception The exception, or null if none
     * @param metadata The metadata
     */
    public LoggingEvent(String correlationId, String service, LogLevel level, String message, Throwable exception, Map<String, Object> metadata) {
        this.correlationId = correlationId;
        this.service = service;
        this.level = level;
        this.message = message;
        this.exception = exception;
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Gets the correlation ID.
     *
     * @return The correlation ID
     */
    public String getCorrelationId() {
        return correlationId;
    }

    /**
     * Gets the service name.
     *
     * @return The service name
     */
    public String getService() {
        return service;
    }

    /**
     * Gets the log level.
     *
     * @return The log level
     */
    public LogLevel getLevel() {
        return level;
    }

    /**
     * Gets the message.
     *
     * @return The message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Gets the exception.
     *
     * @return The exception, or null if none
     */
    public Throwable getException() {
        return exception;
    }

    /**
     * Gets the metadata.
     *
     * @return The metadata
     */
    public Map<String, Object> getMetadata() {
        return new HashMap<>(metadata);
    }

    /**
     * Gets the timestamp.
     *
     * @return The timestamp
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}

