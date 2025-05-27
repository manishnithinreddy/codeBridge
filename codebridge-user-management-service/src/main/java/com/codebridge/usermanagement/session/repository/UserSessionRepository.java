package com.codebridge.usermanagement.session.repository;

import com.codebridge.usermanagement.session.model.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for UserSession entity operations.
 */
@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

    /**
     * Find a session by token.
     *
     * @param token The token
     * @return The session
     */
    Optional<UserSession> findByToken(String token);

    /**
     * Find all active sessions for a user.
     *
     * @param userId The user ID
     * @return The sessions
     */
    List<UserSession> findByUserIdAndActiveTrue(UUID userId);

    /**
     * Find all expired sessions.
     *
     * @param now The current time
     * @return The sessions
     */
    List<UserSession> findByExpiresAtBeforeAndActiveTrue(LocalDateTime now);

    /**
     * Deactivate all sessions for a user.
     *
     * @param userId The user ID
     * @return The number of sessions deactivated
     */
    @Modifying
    @Query("UPDATE UserSession s SET s.active = false WHERE s.userId = :userId AND s.active = true")
    int deactivateAllUserSessions(UUID userId);

    /**
     * Deactivate all expired sessions.
     *
     * @param now The current time
     * @return The number of sessions deactivated
     */
    @Modifying
    @Query("UPDATE UserSession s SET s.active = false WHERE s.expiresAt < :now AND s.active = true")
    int deactivateExpiredSessions(LocalDateTime now);

    /**
     * Count active sessions for a user.
     *
     * @param userId The user ID
     * @return The number of active sessions
     */
    long countByUserIdAndActiveTrue(UUID userId);
}

