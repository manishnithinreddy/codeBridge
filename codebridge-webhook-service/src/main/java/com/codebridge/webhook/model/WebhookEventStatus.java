package com.codebridge.webhook.model;

/**
 * Enum representing the status of a webhook event.
 */
public enum WebhookEventStatus {
    /**
     * The event is pending processing.
     */
    PENDING,
    
    /**
     * The event is being processed.
     */
    PROCESSING,
    
    /**
     * The event was processed successfully.
     */
    PROCESSED,
    
    /**
     * The event processing failed.
     */
    FAILED,
    
    /**
     * The event is scheduled for retry.
     */
    RETRY,
    
    /**
     * The event was rejected due to validation failure.
     */
    REJECTED
}

