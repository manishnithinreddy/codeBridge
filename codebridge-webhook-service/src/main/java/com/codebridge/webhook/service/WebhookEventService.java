package com.codebridge.webhook.service;

import com.codebridge.core.audit.AuditEventPublisher;
import com.codebridge.core.exception.ResourceNotFoundException;
import com.codebridge.webhook.model.Webhook;
import com.codebridge.webhook.model.WebhookEvent;
import com.codebridge.webhook.model.WebhookEventStatus;
import com.codebridge.webhook.repository.WebhookEventRepository;
import com.codebridge.webhook.repository.WebhookRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for webhook event operations.
 */
@Service
public class WebhookEventService {

    private static final Logger logger = LoggerFactory.getLogger(WebhookEventService.class);
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";

    private final WebhookEventRepository webhookEventRepository;
    private final WebhookRepository webhookRepository;
    private final AuditEventPublisher auditEventPublisher;
    private final WebhookSenderService webhookSenderService;

    public WebhookEventService(WebhookEventRepository webhookEventRepository,
                               WebhookRepository webhookRepository,
                               AuditEventPublisher auditEventPublisher,
                               WebhookSenderService webhookSenderService) {
        this.webhookEventRepository = webhookEventRepository;
        this.webhookRepository = webhookRepository;
        this.auditEventPublisher = auditEventPublisher;
        this.webhookSenderService = webhookSenderService;
    }

    /**
     * Creates a new webhook event.
     *
     * @param webhookId the webhook ID
     * @param eventType the event type
     * @param payload the event payload
     * @param signature the event signature
     * @param sourceIp the source IP
     * @param userAgent the user agent
     * @return the created webhook event
     */
    @Transactional
    public Mono<WebhookEvent> createWebhookEvent(UUID webhookId, String eventType, String payload,
                                                String signature, String sourceIp, String userAgent) {
        return Mono.fromCallable(() -> {
            Webhook webhook = webhookRepository.findByIdActive(webhookId)
                    .orElseThrow(() -> new ResourceNotFoundException("Webhook", "id", webhookId.toString()));
            
            WebhookEvent webhookEvent = new WebhookEvent();
            webhookEvent.setId(UUID.randomUUID());
            webhookEvent.setWebhookId(webhookId);
            webhookEvent.setEventType(eventType);
            webhookEvent.setPayload(payload);
            webhookEvent.setSignature(signature);
            webhookEvent.setSourceIp(sourceIp);
            webhookEvent.setUserAgent(userAgent);
            webhookEvent.setStatus(WebhookEventStatus.PENDING);
            webhookEvent.setCorrelationId(MDC.get(CORRELATION_ID_MDC_KEY));
            
            WebhookEvent savedEvent = webhookEventRepository.save(webhookEvent);
            
            // Audit the webhook event creation
            Map<String, Object> metadata = Map.of(
                    "webhookId", webhook.getId().toString(),
                    "webhookName", webhook.getName(),
                    "webhookType", webhook.getType().toString(),
                    "eventType", eventType,
                    "eventId", savedEvent.getId().toString()
            );
            
            auditEventPublisher.publishAuditEvent(
                    "WEBHOOK_EVENT_RECEIVED",
                    "/api/webhooks/" + webhookId + "/events",
                    "POST",
                    webhook.getUserId(),
                    webhook.getTeamId(),
                    "SUCCESS",
                    null,
                    null,
                    metadata
            );
            
            logger.info("Created webhook event: {}", savedEvent.getId());
            
            // Process the event asynchronously
            processWebhookEvent(savedEvent.getId());
            
            return savedEvent;
        });
    }

    /**
     * Gets a webhook event by ID.
     *
     * @param id the webhook event ID
     * @return the webhook event
     */
    @Transactional(readOnly = true)
    public Mono<WebhookEvent> getWebhookEvent(UUID id) {
        return Mono.fromCallable(() -> webhookEventRepository.findByIdActive(id)
                .orElseThrow(() -> new ResourceNotFoundException("WebhookEvent", "id", id.toString())));
    }

    /**
     * Gets all webhook events for a webhook.
     *
     * @param webhookId the webhook ID
     * @param pageable the pagination information
     * @return page of webhook events
     */
    @Transactional(readOnly = true)
    public Mono<Page<WebhookEvent>> getWebhookEvents(UUID webhookId, Pageable pageable) {
        return Mono.fromCallable(() -> webhookEventRepository.findByWebhookIdAndDeletedFalse(webhookId, pageable));
    }

    /**
     * Gets all webhook events for a webhook with a specific status.
     *
     * @param webhookId the webhook ID
     * @param status the event status
     * @param pageable the pagination information
     * @return page of webhook events
     */
    @Transactional(readOnly = true)
    public Mono<Page<WebhookEvent>> getWebhookEventsByStatus(UUID webhookId, WebhookEventStatus status, Pageable pageable) {
        return Mono.fromCallable(() -> webhookEventRepository.findByWebhookIdAndStatusAndDeletedFalse(webhookId, status, pageable));
    }

    /**
     * Gets all webhook events with a specific status.
     *
     * @param status the event status
     * @param pageable the pagination information
     * @return page of webhook events
     */
    @Transactional(readOnly = true)
    public Mono<Page<WebhookEvent>> getAllWebhookEventsByStatus(WebhookEventStatus status, Pageable pageable) {
        return Mono.fromCallable(() -> webhookEventRepository.findByStatusAndDeletedFalse(status, pageable));
    }

    /**
     * Gets all webhook events for a webhook with a specific event type.
     *
     * @param webhookId the webhook ID
     * @param eventType the event type
     * @param pageable the pagination information
     * @return page of webhook events
     */
    @Transactional(readOnly = true)
    public Mono<Page<WebhookEvent>> getWebhookEventsByType(UUID webhookId, String eventType, Pageable pageable) {
        return Mono.fromCallable(() -> webhookEventRepository.findByWebhookIdAndEventTypeAndDeletedFalse(webhookId, eventType, pageable));
    }

    /**
     * Processes a webhook event.
     *
     * @param eventId the event ID
     */
    @Async
    @Transactional
    public void processWebhookEvent(UUID eventId) {
        try {
            WebhookEvent event = webhookEventRepository.findByIdActive(eventId)
                    .orElseThrow(() -> new ResourceNotFoundException("WebhookEvent", "id", eventId.toString()));
            
            Webhook webhook = webhookRepository.findByIdActive(event.getWebhookId())
                    .orElseThrow(() -> new ResourceNotFoundException("Webhook", "id", event.getWebhookId().toString()));
            
            // Skip processing if the webhook is not active
            if (!webhook.isActive()) {
                event.setStatus(WebhookEventStatus.REJECTED);
                event.setErrorMessage("Webhook is not active");
                webhookEventRepository.save(event);
                return;
            }
            
            // Update event status to processing
            event.setStatus(WebhookEventStatus.PROCESSING);
            webhookEventRepository.save(event);
            
            // Send the webhook event
            boolean success = webhookSenderService.sendWebhookEvent(webhook, event);
            
            // Update event status based on the result
            if (success) {
                event.setStatus(WebhookEventStatus.PROCESSED);
                event.setProcessedAt(LocalDateTime.now());
                webhookEventRepository.save(event);
                
                logger.info("Processed webhook event: {}", event.getId());
            } else {
                handleFailedEvent(event, webhook, "Failed to send webhook event");
            }
        } catch (Exception e) {
            logger.error("Error processing webhook event: {}", eventId, e);
            
            try {
                WebhookEvent event = webhookEventRepository.findByIdActive(eventId)
                        .orElseThrow(() -> new ResourceNotFoundException("WebhookEvent", "id", eventId.toString()));
                
                Webhook webhook = webhookRepository.findByIdActive(event.getWebhookId())
                        .orElseThrow(() -> new ResourceNotFoundException("Webhook", "id", event.getWebhookId().toString()));
                
                handleFailedEvent(event, webhook, e.getMessage());
            } catch (Exception ex) {
                logger.error("Error handling failed webhook event: {}", eventId, ex);
            }
        }
    }

    /**
     * Handles a failed webhook event.
     *
     * @param event the webhook event
     * @param webhook the webhook
     * @param errorMessage the error message
     */
    private void handleFailedEvent(WebhookEvent event, Webhook webhook, String errorMessage) {
        event.setRetryCount(event.getRetryCount() + 1);
        
        if (event.getRetryCount() < webhook.getRetryCount()) {
            // Schedule for retry
            event.setStatus(WebhookEventStatus.RETRY);
            event.setErrorMessage(errorMessage);
            
            // Calculate next retry time with exponential backoff
            int delaySeconds = (int) Math.pow(2, event.getRetryCount());
            event.setNextRetryAt(LocalDateTime.now().plusSeconds(delaySeconds));
            
            logger.info("Scheduled webhook event for retry: {}, retry count: {}, next retry at: {}",
                    event.getId(), event.getRetryCount(), event.getNextRetryAt());
        } else {
            // Max retries reached
            event.setStatus(WebhookEventStatus.FAILED);
            event.setErrorMessage(errorMessage);
            
            logger.warn("Webhook event failed after max retries: {}, retry count: {}",
                    event.getId(), event.getRetryCount());
        }
        
        webhookEventRepository.save(event);
    }

    /**
     * Scheduled task to retry failed webhook events.
     * Runs every minute.
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void retryFailedEvents() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<WebhookEvent> eventsToRetry = webhookEventRepository.findEventsForRetry(now, WebhookEventStatus.RETRY);
            
            logger.info("Found {} webhook events to retry", eventsToRetry.size());
            
            for (WebhookEvent event : eventsToRetry) {
                processWebhookEvent(event.getId());
            }
        } catch (Exception e) {
            logger.error("Error retrying failed webhook events", e);
        }
    }
}

