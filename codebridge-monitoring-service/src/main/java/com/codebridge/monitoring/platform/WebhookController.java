package com.codebridge.monitoring.platform.ops.events.controller;

import com.codebridge.monitoring.platform.ops.events.dto.WebhookDto;
import com.codebridge.monitoring.platform.ops.events.dto.WebhookEventDto;
import com.codebridge.monitoring.platform.ops.events.dto.WebhookRequest;
import com.codebridge.monitoring.platform.ops.events.service.WebhookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for webhook management and event handling.
 * Provides endpoints for webhook CRUD operations and event processing.
 */
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;

    /**
     * Get all webhooks.
     *
     * @return List of webhooks
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('ORGANIZATION_ADMIN')")
    public ResponseEntity<List<WebhookDto>> getAllWebhooks() {
        return ResponseEntity.ok(webhookService.getAllWebhooks());
    }

    /**
     * Get webhooks by organization ID.
     *
     * @param organizationId Organization ID
     * @return List of webhooks
     */
    @GetMapping("/organization/{organizationId}")
    @PreAuthorize("hasRole('ADMIN') or @webhookSecurity.isOrganizationMember(#organizationId)")
    public ResponseEntity<List<WebhookDto>> getWebhooksByOrganization(@PathVariable Long organizationId) {
        return ResponseEntity.ok(webhookService.getWebhooksByOrganization(organizationId));
    }

    /**
     * Get webhook by ID.
     *
     * @param id Webhook ID
     * @return Webhook details
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @webhookSecurity.canAccessWebhook(#id)")
    public ResponseEntity<WebhookDto> getWebhookById(@PathVariable Long id) {
        return ResponseEntity.ok(webhookService.getWebhookById(id));
    }

    /**
     * Create a new webhook.
     *
     * @param request Webhook creation request
     * @return Created webhook details
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('ORGANIZATION_ADMIN')")
    public ResponseEntity<WebhookDto> createWebhook(@Valid @RequestBody WebhookRequest request) {
        return ResponseEntity.ok(webhookService.createWebhook(request));
    }

    /**
     * Update a webhook.
     *
     * @param id Webhook ID
     * @param request Webhook update request
     * @return Updated webhook details
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @webhookSecurity.canManageWebhook(#id)")
    public ResponseEntity<WebhookDto> updateWebhook(
            @PathVariable Long id,
            @Valid @RequestBody WebhookRequest request) {
        return ResponseEntity.ok(webhookService.updateWebhook(id, request));
    }

    /**
     * Delete a webhook.
     *
     * @param id Webhook ID
     * @return Success response
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @webhookSecurity.canManageWebhook(#id)")
    public ResponseEntity<?> deleteWebhook(@PathVariable Long id) {
        webhookService.deleteWebhook(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Get webhook events by webhook ID.
     *
     * @param id Webhook ID
     * @return List of webhook events
     */
    @GetMapping("/{id}/events")
    @PreAuthorize("hasRole('ADMIN') or @webhookSecurity.canAccessWebhook(#id)")
    public ResponseEntity<List<WebhookEventDto>> getWebhookEvents(@PathVariable Long id) {
        return ResponseEntity.ok(webhookService.getWebhookEvents(id));
    }

    /**
     * Trigger a webhook test event.
     *
     * @param id Webhook ID
     * @return Success response
     */
    @PostMapping("/{id}/test")
    @PreAuthorize("hasRole('ADMIN') or @webhookSecurity.canManageWebhook(#id)")
    public ResponseEntity<?> testWebhook(@PathVariable Long id) {
        webhookService.testWebhook(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Retry a failed webhook event.
     *
     * @param id Webhook ID
     * @param eventId Event ID
     * @return Success response
     */
    @PostMapping("/{id}/events/{eventId}/retry")
    @PreAuthorize("hasRole('ADMIN') or @webhookSecurity.canManageWebhook(#id)")
    public ResponseEntity<?> retryWebhookEvent(@PathVariable Long id, @PathVariable Long eventId) {
        webhookService.retryWebhookEvent(id, eventId);
        return ResponseEntity.ok().build();
    }
}

