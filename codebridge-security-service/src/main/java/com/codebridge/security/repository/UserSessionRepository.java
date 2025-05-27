package com.codebridge.security.repository;

import com.codebridge.core.repository.BaseRepository;
import com.codebridge.security.model.UserSession;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for UserSession entity operations.
 */
@Repository
public interface UserSessionRepository extends BaseRepository<UserSession, UUID> {

    /**
     * Find a session by its token.
     *
     * @param sessionToken the session token
     * @return the session if found
     */
    Optional<UserSession> findBySessionTokenAndActiveTrue(String sessionToken);

    /**
     * Find all active sessions for a user.
     *
     * @param userId the user ID
     * @return list of active sessions
     */
    List<UserSession> findByUserIdAndActiveTrueAndDeletedFalse(UUID userId);

    /**
     * Find a session by its refresh token.
     *
     * @param refreshToken the refresh token
     * @return the session if found
     */
    Optional<UserSession> findByRefreshTokenAndActiveTrue(String refreshToken);

    /**
     * Deactivate all sessions for a user.
     *
     * @param userId the user ID
     * @return the number of deactivated sessions
     */
    @Modifying
    @Query("UPDATE UserSession s SET s.active = false WHERE s.userId = :userId AND s.active = true")
    int deactivateAllUserSessions(@Param("userId") UUID userId);

    /**
     * Deactivate a specific session.
     *
     * @param sessionToken the session token
     * @return the number of deactivated sessions
     */
    @Modifying
    @Query("UPDATE UserSession s SET s.active = false WHERE s.sessionToken = :sessionToken AND s.active = true")
    int deactivateSession(@Param("sessionToken") String sessionToken);

    /**
     * Delete all expired sessions.
     *
     * @param now the current time
     * @return the number of deleted sessions
     */
    @Modifying
    @Query("DELETE FROM UserSession s WHERE s.expiresAt < :now")
    int deleteAllExpiredSessions(@Param("now") Instant now);

    /**
     * Update the last activity time for a session.
     *
     * @param sessionToken the session token
     * @param lastActivityAt the last activity time
     * @return the number of updated sessions
     */
    @Modifying
    @Query("UPDATE UserSession s SET s.lastActivityAt = :lastActivityAt WHERE s.sessionToken = :sessionToken AND s.active = true")
    int updateLastActivity(@Param("sessionToken") String sessionToken, @Param("lastActivityAt") Instant lastActivityAt);

    /**
     * Count active sessions for a user.
     *
     * @param userId the user ID
     * @return the number of active sessions
     */
    long countByUserIdAndActiveTrue(UUID userId);
}

