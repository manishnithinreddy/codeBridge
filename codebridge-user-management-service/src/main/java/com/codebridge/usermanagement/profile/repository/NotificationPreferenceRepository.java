package com.codebridge.usermanagement.profile.repository;

import com.codebridge.usermanagement.profile.model.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for NotificationPreference entity operations.
 */
@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, UUID> {

    /**
     * Find all notification preferences for a user.
     *
     * @param userId the user ID
     * @return list of notification preferences
     */
    List<NotificationPreference> findByUserId(UUID userId);

    /**
     * Find a specific notification preference for a user.
     *
     * @param userId the user ID
     * @param eventType the event type
     * @return the notification preference if found
     */
    Optional<NotificationPreference> findByUserIdAndEventType(UUID userId, String eventType);

    /**
     * Find all notification preferences for a user with email enabled.
     *
     * @param userId the user ID
     * @param emailEnabled the email enabled status
     * @return list of notification preferences
     */
    List<NotificationPreference> findByUserIdAndEmailEnabled(UUID userId, boolean emailEnabled);

    /**
     * Find all notification preferences for a user with push enabled.
     *
     * @param userId the user ID
     * @param pushEnabled the push enabled status
     * @return list of notification preferences
     */
    List<NotificationPreference> findByUserIdAndPushEnabled(UUID userId, boolean pushEnabled);

    /**
     * Find all notification preferences for a user with in-app enabled.
     *
     * @param userId the user ID
     * @param inAppEnabled the in-app enabled status
     * @return list of notification preferences
     */
    List<NotificationPreference> findByUserIdAndInAppEnabled(UUID userId, boolean inAppEnabled);

    /**
     * Find all notification preferences for a user with Slack enabled.
     *
     * @param userId the user ID
     * @param slackEnabled the Slack enabled status
     * @return list of notification preferences
     */
    List<NotificationPreference> findByUserIdAndSlackEnabled(UUID userId, boolean slackEnabled);

    /**
     * Delete all notification preferences for a user.
     *
     * @param userId the user ID
     * @return the number of deleted notification preferences
     */
    int deleteByUserId(UUID userId);
}
