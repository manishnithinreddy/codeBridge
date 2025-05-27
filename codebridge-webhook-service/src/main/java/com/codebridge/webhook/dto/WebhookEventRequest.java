package com.codebridge.webhook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for webhook event requests.
 */
public class WebhookEventRequest {

    @NotBlank(message = "Event type is required")
    private String eventType;

    @NotNull(message = "Payload is required")
    private String payload;

    public WebhookEventRequest() {
    }

    public WebhookEventRequest(String eventType, String payload) {
        this.eventType = eventType;
        this.payload = payload;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}

