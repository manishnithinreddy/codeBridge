package com.codebridge.events.model;

/**
 * Enum representing the possible statuses of a webhook event.
 */
public enum WebhookEventStatus {
    PENDING,
    PROCESSING,
    DELIVERED,
    FAILED,
    RETRYING
}

