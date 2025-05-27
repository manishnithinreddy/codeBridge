package com.codebridge.webhook.dto;

import com.codebridge.webhook.model.WebhookEventStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for webhook event responses.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WebhookEventResponse {

    private UUID id;
    private UUID webhookId;
    private String eventType;
    private WebhookEventStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
    private int retryCount;
    private LocalDateTime nextRetryAt;
    private String errorMessage;

    public WebhookEventResponse() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getWebhookId() {
        return webhookId;
    }

    public void setWebhookId(UUID webhookId) {
        this.webhookId = webhookId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public WebhookEventStatus getStatus() {
        return status;
    }

    public void setStatus(WebhookEventStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public LocalDateTime getNextRetryAt() {
        return nextRetryAt;
    }

    public void setNextRetryAt(LocalDateTime nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}

