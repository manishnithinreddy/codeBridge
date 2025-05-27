package com.codebridge.webhook.controller;

import com.codebridge.core.security.SecuredMethod;
import com.codebridge.webhook.dto.WebhookEventRequest;
import com.codebridge.webhook.dto.WebhookEventResponse;
import com.codebridge.webhook.model.WebhookEvent;
import com.codebridge.webhook.model.WebhookEventStatus;
import com.codebridge.webhook.service.WebhookEventService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Controller for webhook event operations.
 */
@RestController
@RequestMapping("/api/webhooks")
public class WebhookEventController {

    private final WebhookEventService webhookEventService;

    public WebhookEventController(WebhookEventService webhookEventService) {
        this.webhookEventService = webhookEventService;
    }

    /**
     * Creates a new webhook event.
     *
     * @param webhookId the webhook ID
     * @param request the webhook event request
     * @param httpRequest the HTTP request
     * @return the created webhook event
     */
    @PostMapping("/{webhookId}/events")
    public Mono<ResponseEntity<WebhookEventResponse>> createWebhookEvent(
            @PathVariable UUID webhookId,
            @Valid @RequestBody WebhookEventRequest request,
            HttpServletRequest httpRequest) {
        
        String signature = httpRequest.getHeader("X-Webhook-Signature");
        String sourceIp = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");
        
        return webhookEventService.createWebhookEvent(
                webhookId,
                request.getEventType(),
                request.getPayload(),
                signature,
                sourceIp,
                userAgent
        ).map(event -> ResponseEntity.ok(mapToResponse(event)));
    }

    /**
     * Gets a webhook event by ID.
     *
     * @param id the webhook event ID
     * @return the webhook event
     */
    @GetMapping("/events/{id}")
    @SecuredMethod(roles = {"ROLE_USER", "ROLE_ADMIN"})
    public Mono<ResponseEntity<WebhookEventResponse>> getWebhookEvent(@PathVariable UUID id) {
        return webhookEventService.getWebhookEvent(id)
                .map(event -> ResponseEntity.ok(mapToResponse(event)));
    }

    /**
     * Gets all webhook events for a webhook.
     *
     * @param webhookId the webhook ID
     * @param pageable the pagination information
     * @return page of webhook events
     */
    @GetMapping("/{webhookId}/events")
    @SecuredMethod(roles = {"ROLE_USER", "ROLE_ADMIN"})
    public Mono<ResponseEntity<Page<WebhookEventResponse>>> getWebhookEvents(
            @PathVariable UUID webhookId,
            Pageable pageable) {
        
        return webhookEventService.getWebhookEvents(webhookId, pageable)
                .map(page -> page.map(this::mapToResponse))
                .map(ResponseEntity::ok);
    }

    /**
     * Gets all webhook events for a webhook with a specific status.
     *
     * @param webhookId the webhook ID
     * @param status the event status
     * @param pageable the pagination information
     * @return page of webhook events
     */
    @GetMapping("/{webhookId}/events/status/{status}")
    @SecuredMethod(roles = {"ROLE_USER", "ROLE_ADMIN"})
    public Mono<ResponseEntity<Page<WebhookEventResponse>>> getWebhookEventsByStatus(
            @PathVariable UUID webhookId,
            @PathVariable WebhookEventStatus status,
            Pageable pageable) {
        
        return webhookEventService.getWebhookEventsByStatus(webhookId, status, pageable)
                .map(page -> page.map(this::mapToResponse))
                .map(ResponseEntity::ok);
    }

    /**
     * Gets all webhook events with a specific status.
     *
     * @param status the event status
     * @param pageable the pagination information
     * @return page of webhook events
     */
    @GetMapping("/events/status/{status}")
    @SecuredMethod(roles = {"ROLE_ADMIN"})
    public Mono<ResponseEntity<Page<WebhookEventResponse>>> getAllWebhookEventsByStatus(
            @PathVariable WebhookEventStatus status,
            Pageable pageable) {
        
        return webhookEventService.getAllWebhookEventsByStatus(status, pageable)
                .map(page -> page.map(this::mapToResponse))
                .map(ResponseEntity::ok);
    }

    /**
     * Gets all webhook events for a webhook with a specific event type.
     *
     * @param webhookId the webhook ID
     * @param eventType the event type
     * @param pageable the pagination information
     * @return page of webhook events
     */
    @GetMapping("/{webhookId}/events/type")
    @SecuredMethod(roles = {"ROLE_USER", "ROLE_ADMIN"})
    public Mono<ResponseEntity<Page<WebhookEventResponse>>> getWebhookEventsByType(
            @PathVariable UUID webhookId,
            @RequestParam String eventType,
            Pageable pageable) {
        
        return webhookEventService.getWebhookEventsByType(webhookId, eventType, pageable)
                .map(page -> page.map(this::mapToResponse))
                .map(ResponseEntity::ok);
    }

    /**
     * Maps a webhook event entity to a webhook event response DTO.
     *
     * @param event the webhook event entity
     * @return the webhook event response DTO
     */
    private WebhookEventResponse mapToResponse(WebhookEvent event) {
        WebhookEventResponse response = new WebhookEventResponse();
        response.setId(event.getId());
        response.setWebhookId(event.getWebhookId());
        response.setEventType(event.getEventType());
        response.setStatus(event.getStatus());
        response.setCreatedAt(event.getCreatedAt());
        response.setProcessedAt(event.getProcessedAt());
        response.setRetryCount(event.getRetryCount());
        response.setNextRetryAt(event.getNextRetryAt());
        response.setErrorMessage(event.getErrorMessage());
        
        return response;
    }
}

