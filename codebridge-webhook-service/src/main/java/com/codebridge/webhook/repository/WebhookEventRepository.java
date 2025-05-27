package com.codebridge.webhook.repository;

import com.codebridge.core.repository.BaseRepository;
import com.codebridge.webhook.model.WebhookEvent;
import com.codebridge.webhook.model.WebhookEventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for webhook event operations.
 */
@Repository
public interface WebhookEventRepository extends BaseRepository<WebhookEvent, UUID> {

    /**
     * Finds all events for a webhook.
     *
     * @param webhookId the webhook ID
     * @param pageable the pagination information
     * @return page of webhook events
     */
    Page<WebhookEvent> findByWebhookIdAndDeletedFalse(UUID webhookId, Pageable pageable);

    /**
     * Finds all events for a webhook with a specific status.
     *
     * @param webhookId the webhook ID
     * @param status the event status
     * @param pageable the pagination information
     * @return page of webhook events
     */
    Page<WebhookEvent> findByWebhookIdAndStatusAndDeletedFalse(UUID webhookId, WebhookEventStatus status, Pageable pageable);

    /**
     * Finds all events with a specific status.
     *
     * @param status the event status
     * @param pageable the pagination information
     * @return page of webhook events
     */
    Page<WebhookEvent> findByStatusAndDeletedFalse(WebhookEventStatus status, Pageable pageable);

    /**
     * Finds all events scheduled for retry.
     *
     * @param now the current time
     * @param status the event status
     * @return list of webhook events
     */
    @Query("SELECT e FROM WebhookEvent e WHERE e.status = :status AND e.nextRetryAt <= :now AND e.deleted = false")
    List<WebhookEvent> findEventsForRetry(@Param("now") LocalDateTime now, @Param("status") WebhookEventStatus status);

    /**
     * Finds all events for a webhook with a specific event type.
     *
     * @param webhookId the webhook ID
     * @param eventType the event type
     * @param pageable the pagination information
     * @return page of webhook events
     */
    Page<WebhookEvent> findByWebhookIdAndEventTypeAndDeletedFalse(UUID webhookId, String eventType, Pageable pageable);

    /**
     * Finds all events with a specific correlation ID.
     *
     * @param correlationId the correlation ID
     * @return list of webhook events
     */
    List<WebhookEvent> findByCorrelationIdAndDeletedFalse(String correlationId);
}

