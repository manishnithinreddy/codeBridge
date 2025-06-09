package com.codebridge.usermanagement.profile.repository;

import com.codebridge.usermanagement.profile.model.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for notification preferences.
 */
@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, UUID> {

    /**
     * Finds a notification preference by user ID and event type.
     *
     * @param userId the user ID
     * @param eventType the event type
     * @return the notification preference, if found
     */
    Optional<NotificationPreference> findByUserIdAndEventType(UUID userId, String eventType);

    /**
     * Finds all notification preferences for a user.
     *
     * @param userId the user ID
     * @return the list of notification preferences
     */
    List<NotificationPreference> findByUserId(UUID userId);
}

