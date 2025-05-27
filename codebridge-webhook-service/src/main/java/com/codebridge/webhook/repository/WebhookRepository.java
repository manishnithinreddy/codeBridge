package com.codebridge.webhook.repository;

import com.codebridge.core.repository.BaseRepository;
import com.codebridge.webhook.model.Webhook;
import com.codebridge.webhook.model.WebhookType;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for webhook operations.
 */
@Repository
public interface WebhookRepository extends BaseRepository<Webhook, UUID> {

    /**
     * Finds all active webhooks for a team.
     *
     * @param teamId the team ID
     * @return list of active webhooks
     */
    List<Webhook> findByTeamIdAndActiveAndDeletedFalse(UUID teamId, boolean active);

    /**
     * Finds all active webhooks for a user.
     *
     * @param userId the user ID
     * @return list of active webhooks
     */
    List<Webhook> findByUserIdAndActiveAndDeletedFalse(UUID userId, boolean active);

    /**
     * Finds all active webhooks of a specific type.
     *
     * @param type the webhook type
     * @return list of active webhooks
     */
    List<Webhook> findByTypeAndActiveAndDeletedFalse(WebhookType type, boolean active);

    /**
     * Finds all active webhooks for a team and type.
     *
     * @param teamId the team ID
     * @param type the webhook type
     * @return list of active webhooks
     */
    List<Webhook> findByTeamIdAndTypeAndActiveAndDeletedFalse(UUID teamId, WebhookType type, boolean active);

    /**
     * Finds all active webhooks for a user and type.
     *
     * @param userId the user ID
     * @param type the webhook type
     * @return list of active webhooks
     */
    List<Webhook> findByUserIdAndTypeAndActiveAndDeletedFalse(UUID userId, WebhookType type, boolean active);
}

