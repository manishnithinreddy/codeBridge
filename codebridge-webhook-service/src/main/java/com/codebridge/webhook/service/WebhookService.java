package com.codebridge.webhook.service;

import com.codebridge.core.audit.AuditEventPublisher;
import com.codebridge.core.exception.ResourceNotFoundException;
import com.codebridge.core.security.SecretStorageService;
import com.codebridge.webhook.model.Webhook;
import com.codebridge.webhook.model.WebhookType;
import com.codebridge.webhook.repository.WebhookRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for webhook operations.
 */
@Service
public class WebhookService {

    private static final Logger logger = LoggerFactory.getLogger(WebhookService.class);

    private final WebhookRepository webhookRepository;
    private final SecretStorageService secretStorageService;
    private final AuditEventPublisher auditEventPublisher;

    public WebhookService(WebhookRepository webhookRepository,
                          SecretStorageService secretStorageService,
                          AuditEventPublisher auditEventPublisher) {
        this.webhookRepository = webhookRepository;
        this.secretStorageService = secretStorageService;
        this.auditEventPublisher = auditEventPublisher;
    }

    /**
     * Creates a new webhook.
     *
     * @param webhook the webhook to create
     * @return the created webhook
     */
    @Transactional
    public Mono<Webhook> createWebhook(Webhook webhook) {
        return Mono.fromCallable(() -> {
            webhook.setId(UUID.randomUUID());
            
            // Encrypt the secret if present
            if (webhook.getSecret() != null && !webhook.getSecret().isEmpty()) {
                webhook.setSecret(secretStorageService.encryptSecret(webhook.getSecret()));
            }
            
            Webhook savedWebhook = webhookRepository.save(webhook);
            
            // Audit the webhook creation
            Map<String, Object> metadata = Map.of(
                    "webhookId", savedWebhook.getId().toString(),
                    "webhookName", savedWebhook.getName(),
                    "webhookType", savedWebhook.getType().toString()
            );
            
            auditEventPublisher.publishAuditEvent(
                    "WEBHOOK_CREATED",
                    "/api/webhooks",
                    "POST",
                    savedWebhook.getUserId(),
                    savedWebhook.getTeamId(),
                    "SUCCESS",
                    null,
                    null,
                    metadata
            );
            
            logger.info("Created webhook: {}", savedWebhook.getId());
            
            return savedWebhook;
        });
    }

    /**
     * Updates an existing webhook.
     *
     * @param id the webhook ID
     * @param webhook the updated webhook
     * @return the updated webhook
     */
    @Transactional
    public Mono<Webhook> updateWebhook(UUID id, Webhook webhook) {
        return Mono.fromCallable(() -> {
            Webhook existingWebhook = webhookRepository.findByIdActive(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Webhook", "id", id.toString()));
            
            existingWebhook.setName(webhook.getName());
            existingWebhook.setDescription(webhook.getDescription());
            existingWebhook.setUrl(webhook.getUrl());
            existingWebhook.setActive(webhook.isActive());
            existingWebhook.setType(webhook.getType());
            existingWebhook.setEvents(webhook.getEvents());
            existingWebhook.setHeaders(webhook.getHeaders());
            existingWebhook.setRetryCount(webhook.getRetryCount());
            existingWebhook.setTimeoutSeconds(webhook.getTimeoutSeconds());
            
            // Only update the secret if a new one is provided
            if (webhook.getSecret() != null && !webhook.getSecret().isEmpty()) {
                existingWebhook.setSecret(secretStorageService.encryptSecret(webhook.getSecret()));
            }
            
            Webhook updatedWebhook = webhookRepository.save(existingWebhook);
            
            // Audit the webhook update
            Map<String, Object> metadata = Map.of(
                    "webhookId", updatedWebhook.getId().toString(),
                    "webhookName", updatedWebhook.getName(),
                    "webhookType", updatedWebhook.getType().toString()
            );
            
            auditEventPublisher.publishAuditEvent(
                    "WEBHOOK_UPDATED",
                    "/api/webhooks/" + id,
                    "PUT",
                    updatedWebhook.getUserId(),
                    updatedWebhook.getTeamId(),
                    "SUCCESS",
                    null,
                    null,
                    metadata
            );
            
            logger.info("Updated webhook: {}", updatedWebhook.getId());
            
            return updatedWebhook;
        });
    }

    /**
     * Gets a webhook by ID.
     *
     * @param id the webhook ID
     * @return the webhook
     */
    @Transactional(readOnly = true)
    public Mono<Webhook> getWebhook(UUID id) {
        return Mono.fromCallable(() -> webhookRepository.findByIdActive(id)
                .orElseThrow(() -> new ResourceNotFoundException("Webhook", "id", id.toString())));
    }

    /**
     * Gets all webhooks for a team.
     *
     * @param teamId the team ID
     * @param active whether to get only active webhooks
     * @return list of webhooks
     */
    @Transactional(readOnly = true)
    public Flux<Webhook> getWebhooksByTeam(UUID teamId, boolean active) {
        return Flux.fromIterable(webhookRepository.findByTeamIdAndActiveAndDeletedFalse(teamId, active));
    }

    /**
     * Gets all webhooks for a user.
     *
     * @param userId the user ID
     * @param active whether to get only active webhooks
     * @return list of webhooks
     */
    @Transactional(readOnly = true)
    public Flux<Webhook> getWebhooksByUser(UUID userId, boolean active) {
        return Flux.fromIterable(webhookRepository.findByUserIdAndActiveAndDeletedFalse(userId, active));
    }

    /**
     * Gets all webhooks of a specific type.
     *
     * @param type the webhook type
     * @param active whether to get only active webhooks
     * @return list of webhooks
     */
    @Transactional(readOnly = true)
    public Flux<Webhook> getWebhooksByType(WebhookType type, boolean active) {
        return Flux.fromIterable(webhookRepository.findByTypeAndActiveAndDeletedFalse(type, active));
    }

    /**
     * Gets all webhooks for a team and type.
     *
     * @param teamId the team ID
     * @param type the webhook type
     * @param active whether to get only active webhooks
     * @return list of webhooks
     */
    @Transactional(readOnly = true)
    public Flux<Webhook> getWebhooksByTeamAndType(UUID teamId, WebhookType type, boolean active) {
        return Flux.fromIterable(webhookRepository.findByTeamIdAndTypeAndActiveAndDeletedFalse(teamId, type, active));
    }

    /**
     * Gets all webhooks for a user and type.
     *
     * @param userId the user ID
     * @param type the webhook type
     * @param active whether to get only active webhooks
     * @return list of webhooks
     */
    @Transactional(readOnly = true)
    public Flux<Webhook> getWebhooksByUserAndType(UUID userId, WebhookType type, boolean active) {
        return Flux.fromIterable(webhookRepository.findByUserIdAndTypeAndActiveAndDeletedFalse(userId, type, active));
    }

    /**
     * Gets all webhooks.
     *
     * @param pageable the pagination information
     * @return page of webhooks
     */
    @Transactional(readOnly = true)
    public Mono<Page<Webhook>> getAllWebhooks(Pageable pageable) {
        return Mono.fromCallable(() -> webhookRepository.findAllActive(pageable));
    }

    /**
     * Deletes a webhook.
     *
     * @param id the webhook ID
     * @param userId the user ID performing the deletion
     * @return true if the webhook was deleted, false otherwise
     */
    @Transactional
    public Mono<Boolean> deleteWebhook(UUID id, UUID userId) {
        return Mono.fromCallable(() -> {
            Webhook webhook = webhookRepository.findByIdActive(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Webhook", "id", id.toString()));
            
            int result = webhookRepository.softDelete(id, userId);
            
            if (result > 0) {
                // Audit the webhook deletion
                Map<String, Object> metadata = Map.of(
                        "webhookId", webhook.getId().toString(),
                        "webhookName", webhook.getName(),
                        "webhookType", webhook.getType().toString()
                );
                
                auditEventPublisher.publishAuditEvent(
                        "WEBHOOK_DELETED",
                        "/api/webhooks/" + id,
                        "DELETE",
                        userId,
                        webhook.getTeamId(),
                        "SUCCESS",
                        null,
                        null,
                        metadata
                );
                
                logger.info("Deleted webhook: {}", id);
                
                return true;
            }
            
            return false;
        });
    }

    /**
     * Activates or deactivates a webhook.
     *
     * @param id the webhook ID
     * @param active whether to activate or deactivate the webhook
     * @return the updated webhook
     */
    @Transactional
    public Mono<Webhook> setWebhookActive(UUID id, boolean active) {
        return Mono.fromCallable(() -> {
            Webhook webhook = webhookRepository.findByIdActive(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Webhook", "id", id.toString()));
            
            webhook.setActive(active);
            
            Webhook updatedWebhook = webhookRepository.save(webhook);
            
            // Audit the webhook activation/deactivation
            Map<String, Object> metadata = Map.of(
                    "webhookId", updatedWebhook.getId().toString(),
                    "webhookName", updatedWebhook.getName(),
                    "webhookType", updatedWebhook.getType().toString(),
                    "active", active
            );
            
            auditEventPublisher.publishAuditEvent(
                    active ? "WEBHOOK_ACTIVATED" : "WEBHOOK_DEACTIVATED",
                    "/api/webhooks/" + id + "/active",
                    "PUT",
                    updatedWebhook.getUserId(),
                    updatedWebhook.getTeamId(),
                    "SUCCESS",
                    null,
                    null,
                    metadata
            );
            
            logger.info("{} webhook: {}", active ? "Activated" : "Deactivated", id);
            
            return updatedWebhook;
        });
    }
}

