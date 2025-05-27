package com.codebridge.webhook.controller;

import com.codebridge.core.security.SecuredMethod;
import com.codebridge.core.security.UserPrincipal;
import com.codebridge.webhook.dto.WebhookCreateRequest;
import com.codebridge.webhook.dto.WebhookResponse;
import com.codebridge.webhook.dto.WebhookUpdateRequest;
import com.codebridge.webhook.model.Webhook;
import com.codebridge.webhook.model.WebhookType;
import com.codebridge.webhook.service.WebhookService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Controller for webhook operations.
 */
@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {

    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    /**
     * Creates a new webhook.
     *
     * @param request the webhook create request
     * @param userPrincipal the authenticated user
     * @return the created webhook
     */
    @PostMapping
    @SecuredMethod(roles = {"ROLE_USER", "ROLE_ADMIN"})
    public Mono<ResponseEntity<WebhookResponse>> createWebhook(
            @Valid @RequestBody WebhookCreateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        Webhook webhook = new Webhook();
        webhook.setName(request.getName());
        webhook.setDescription(request.getDescription());
        webhook.setUrl(request.getUrl());
        webhook.setSecret(request.getSecret());
        webhook.setType(request.getType());
        webhook.setEvents(request.getEvents());
        webhook.setHeaders(request.getHeaders());
        webhook.setActive(request.isActive());
        webhook.setUserId(UUID.fromString(userPrincipal.getId()));
        
        if (request.getTeamId() != null) {
            webhook.setTeamId(UUID.fromString(request.getTeamId()));
        }
        
        if (request.getRetryCount() != null) {
            webhook.setRetryCount(request.getRetryCount());
        }
        
        if (request.getTimeoutSeconds() != null) {
            webhook.setTimeoutSeconds(request.getTimeoutSeconds());
        }
        
        return webhookService.createWebhook(webhook)
                .map(createdWebhook -> ResponseEntity.ok(mapToResponse(createdWebhook)));
    }

    /**
     * Updates an existing webhook.
     *
     * @param id the webhook ID
     * @param request the webhook update request
     * @return the updated webhook
     */
    @PutMapping("/{id}")
    @SecuredMethod(roles = {"ROLE_USER", "ROLE_ADMIN"})
    public Mono<ResponseEntity<WebhookResponse>> updateWebhook(
            @PathVariable UUID id,
            @Valid @RequestBody WebhookUpdateRequest request) {
        
        Webhook webhook = new Webhook();
        webhook.setName(request.getName());
        webhook.setDescription(request.getDescription());
        webhook.setUrl(request.getUrl());
        webhook.setSecret(request.getSecret());
        webhook.setType(request.getType());
        webhook.setEvents(request.getEvents());
        webhook.setHeaders(request.getHeaders());
        webhook.setActive(request.isActive());
        
        if (request.getRetryCount() != null) {
            webhook.setRetryCount(request.getRetryCount());
        }
        
        if (request.getTimeoutSeconds() != null) {
            webhook.setTimeoutSeconds(request.getTimeoutSeconds());
        }
        
        return webhookService.updateWebhook(id, webhook)
                .map(updatedWebhook -> ResponseEntity.ok(mapToResponse(updatedWebhook)));
    }

    /**
     * Gets a webhook by ID.
     *
     * @param id the webhook ID
     * @return the webhook
     */
    @GetMapping("/{id}")
    @SecuredMethod(roles = {"ROLE_USER", "ROLE_ADMIN"})
    public Mono<ResponseEntity<WebhookResponse>> getWebhook(@PathVariable UUID id) {
        return webhookService.getWebhook(id)
                .map(webhook -> ResponseEntity.ok(mapToResponse(webhook)));
    }

    /**
     * Gets all webhooks for a team.
     *
     * @param teamId the team ID
     * @param active whether to get only active webhooks
     * @return list of webhooks
     */
    @GetMapping("/team/{teamId}")
    @SecuredMethod(roles = {"ROLE_USER", "ROLE_ADMIN"})
    public Flux<WebhookResponse> getWebhooksByTeam(
            @PathVariable UUID teamId,
            @RequestParam(defaultValue = "true") boolean active) {
        
        return webhookService.getWebhooksByTeam(teamId, active)
                .map(this::mapToResponse);
    }

    /**
     * Gets all webhooks for a user.
     *
     * @param userId the user ID
     * @param active whether to get only active webhooks
     * @return list of webhooks
     */
    @GetMapping("/user/{userId}")
    @SecuredMethod(roles = {"ROLE_USER", "ROLE_ADMIN"})
    public Flux<WebhookResponse> getWebhooksByUser(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "true") boolean active) {
        
        return webhookService.getWebhooksByUser(userId, active)
                .map(this::mapToResponse);
    }

    /**
     * Gets all webhooks of a specific type.
     *
     * @param type the webhook type
     * @param active whether to get only active webhooks
     * @return list of webhooks
     */
    @GetMapping("/type/{type}")
    @SecuredMethod(roles = {"ROLE_USER", "ROLE_ADMIN"})
    public Flux<WebhookResponse> getWebhooksByType(
            @PathVariable WebhookType type,
            @RequestParam(defaultValue = "true") boolean active) {
        
        return webhookService.getWebhooksByType(type, active)
                .map(this::mapToResponse);
    }

    /**
     * Gets all webhooks.
     *
     * @param pageable the pagination information
     * @return page of webhooks
     */
    @GetMapping
    @SecuredMethod(roles = {"ROLE_ADMIN"})
    public Mono<ResponseEntity<Page<WebhookResponse>>> getAllWebhooks(Pageable pageable) {
        return webhookService.getAllWebhooks(pageable)
                .map(page -> page.map(this::mapToResponse))
                .map(ResponseEntity::ok);
    }

    /**
     * Deletes a webhook.
     *
     * @param id the webhook ID
     * @param userPrincipal the authenticated user
     * @return the response entity
     */
    @DeleteMapping("/{id}")
    @SecuredMethod(roles = {"ROLE_USER", "ROLE_ADMIN"})
    public Mono<ResponseEntity<?>> deleteWebhook(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        return webhookService.deleteWebhook(id, UUID.fromString(userPrincipal.getId()))
                .map(deleted -> {
                    if (deleted) {
                        return ResponseEntity.ok().body("Webhook deleted successfully");
                    } else {
                        return ResponseEntity.badRequest().body("Failed to delete webhook");
                    }
                });
    }

    /**
     * Activates or deactivates a webhook.
     *
     * @param id the webhook ID
     * @param active whether to activate or deactivate the webhook
     * @return the updated webhook
     */
    @PutMapping("/{id}/active")
    @SecuredMethod(roles = {"ROLE_USER", "ROLE_ADMIN"})
    public Mono<ResponseEntity<WebhookResponse>> setWebhookActive(
            @PathVariable UUID id,
            @RequestParam boolean active) {
        
        return webhookService.setWebhookActive(id, active)
                .map(webhook -> ResponseEntity.ok(mapToResponse(webhook)));
    }

    /**
     * Maps a webhook entity to a webhook response DTO.
     *
     * @param webhook the webhook entity
     * @return the webhook response DTO
     */
    private WebhookResponse mapToResponse(Webhook webhook) {
        WebhookResponse response = new WebhookResponse();
        response.setId(webhook.getId());
        response.setName(webhook.getName());
        response.setDescription(webhook.getDescription());
        response.setUrl(webhook.getUrl());
        response.setType(webhook.getType());
        response.setEvents(webhook.getEvents());
        response.setActive(webhook.isActive());
        response.setCreatedAt(webhook.getCreatedAt());
        response.setUpdatedAt(webhook.getUpdatedAt());
        
        if (webhook.getTeamId() != null) {
            response.setTeamId(webhook.getTeamId().toString());
        }
        
        if (webhook.getUserId() != null) {
            response.setUserId(webhook.getUserId().toString());
        }
        
        response.setRetryCount(webhook.getRetryCount());
        response.setTimeoutSeconds(webhook.getTimeoutSeconds());
        
        return response;
    }
}

