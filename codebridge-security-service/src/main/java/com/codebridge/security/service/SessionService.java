package com.codebridge.security.service;

import com.codebridge.core.audit.AuditEventPublisher;
import com.codebridge.security.model.UserSession;
import com.codebridge.security.repository.UserSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Service for managing user sessions.
 */
@Service
public class SessionService {

    private static final Logger logger = LoggerFactory.getLogger(SessionService.class);
    private static final Duration SESSION_TIMEOUT = Duration.ofHours(24);
    private static final Duration SESSION_EXTENSION = Duration.ofHours(1);

    private final UserSessionRepository sessionRepository;
    private final AuditEventPublisher auditEventPublisher;

    public SessionService(UserSessionRepository sessionRepository, AuditEventPublisher auditEventPublisher) {
        this.sessionRepository = sessionRepository;
        this.auditEventPublisher = auditEventPublisher;
    }

    /**
     * Creates a new user session.
     *
     * @param userId the user ID
     * @param ipAddress the IP address
     * @param userAgent the user agent
     * @param deviceInfo the device information
     * @param geoLocation the geo location
     * @param refreshToken the refresh token
     * @return the created session
     */
    @Transactional
    public Mono<UserSession> createSession(UUID userId, String ipAddress, String userAgent, 
                                          String deviceInfo, String geoLocation, String refreshToken) {
        return Mono.fromCallable(() -> {
            UserSession session = new UserSession();
            session.setId(UUID.randomUUID());
            session.setUserId(userId);
            session.setSessionToken(generateSessionToken());
            session.setIpAddress(ipAddress);
            session.setUserAgent(userAgent);
            session.setDeviceInfo(deviceInfo);
            session.setGeoLocation(geoLocation);
            session.setRefreshToken(refreshToken);
            session.setLastActivityAt(Instant.now());
            session.setExpiresAt(Instant.now().plus(SESSION_TIMEOUT));
            session.setActive(true);
            
            UserSession savedSession = sessionRepository.save(session);
            
            // Audit the session creation
            Map<String, Object> metadata = Map.of(
                    "sessionId", savedSession.getId().toString(),
                    "userId", savedSession.getUserId().toString(),
                    "ipAddress", savedSession.getIpAddress(),
                    "userAgent", savedSession.getUserAgent()
            );
            
            auditEventPublisher.publishAuditEvent(
                    "SESSION_CREATED",
                    "/api/sessions",
                    "POST",
                    userId,
                    null,
                    "SUCCESS",
                    null,
                    null,
                    metadata
            );
            
            logger.info("Created session: {}", savedSession.getId());
            
            return savedSession;
        });
    }

    /**
     * Gets a session by its token.
     *
     * @param sessionToken the session token
     * @return the session if found
     */
    @Transactional(readOnly = true)
    public Mono<UserSession> getSessionByToken(String sessionToken) {
        return Mono.fromCallable(() -> sessionRepository.findBySessionTokenAndActiveTrue(sessionToken)
                .orElse(null));
    }

    /**
     * Gets all active sessions for a user.
     *
     * @param userId the user ID
     * @return list of active sessions
     */
    @Transactional(readOnly = true)
    public Flux<UserSession> getUserSessions(UUID userId) {
        return Flux.fromIterable(sessionRepository.findByUserIdAndActiveTrueAndDeletedFalse(userId));
    }

    /**
     * Updates the last activity time for a session.
     *
     * @param sessionToken the session token
     * @return true if the session was updated, false otherwise
     */
    @Transactional
    public Mono<Boolean> updateLastActivity(String sessionToken) {
        return Mono.fromCallable(() -> {
            int result = sessionRepository.updateLastActivity(sessionToken, Instant.now());
            return result > 0;
        });
    }

    /**
     * Extends the expiry time for a session.
     *
     * @param sessionToken the session token
     * @return the updated session
     */
    @Transactional
    public Mono<UserSession> extendSession(String sessionToken) {
        return Mono.fromCallable(() -> {
            UserSession session = sessionRepository.findBySessionTokenAndActiveTrue(sessionToken)
                    .orElse(null);
            
            if (session != null) {
                session.setExpiresAt(Instant.now().plus(SESSION_EXTENSION));
                session.setLastActivityAt(Instant.now());
                return sessionRepository.save(session);
            }
            
            return null;
        });
    }

    /**
     * Deactivates a session.
     *
     * @param sessionToken the session token
     * @param userId the user ID
     * @return true if the session was deactivated, false otherwise
     */
    @Transactional
    public Mono<Boolean> deactivateSession(String sessionToken, UUID userId) {
        return Mono.fromCallable(() -> {
            int result = sessionRepository.deactivateSession(sessionToken);
            
            if (result > 0) {
                // Audit the session deactivation
                Map<String, Object> metadata = Map.of(
                        "sessionToken", sessionToken
                );
                
                auditEventPublisher.publishAuditEvent(
                        "SESSION_DEACTIVATED",
                        "/api/sessions/" + sessionToken,
                        "DELETE",
                        userId,
                        null,
                        "SUCCESS",
                        null,
                        null,
                        metadata
                );
                
                logger.info("Deactivated session: {}", sessionToken);
                
                return true;
            }
            
            return false;
        });
    }

    /**
     * Deactivates all sessions for a user.
     *
     * @param userId the user ID
     * @return the number of deactivated sessions
     */
    @Transactional
    public Mono<Integer> deactivateAllUserSessions(UUID userId) {
        return Mono.fromCallable(() -> {
            int result = sessionRepository.deactivateAllUserSessions(userId);
            
            if (result > 0) {
                // Audit the session deactivation
                Map<String, Object> metadata = Map.of(
                        "userId", userId.toString(),
                        "sessionCount", result
                );
                
                auditEventPublisher.publishAuditEvent(
                        "ALL_SESSIONS_DEACTIVATED",
                        "/api/sessions/user/" + userId,
                        "DELETE",
                        userId,
                        null,
                        "SUCCESS",
                        null,
                        null,
                        metadata
                );
                
                logger.info("Deactivated {} sessions for user: {}", result, userId);
            }
            
            return result;
        });
    }

    /**
     * Gets a session by its refresh token.
     *
     * @param refreshToken the refresh token
     * @return the session if found
     */
    @Transactional(readOnly = true)
    public Mono<UserSession> getSessionByRefreshToken(String refreshToken) {
        return Mono.fromCallable(() -> sessionRepository.findByRefreshTokenAndActiveTrue(refreshToken)
                .orElse(null));
    }

    /**
     * Counts active sessions for a user.
     *
     * @param userId the user ID
     * @return the number of active sessions
     */
    @Transactional(readOnly = true)
    public Mono<Long> countActiveSessions(UUID userId) {
        return Mono.fromCallable(() -> sessionRepository.countByUserIdAndActiveTrue(userId));
    }

    /**
     * Scheduled task to clean up expired sessions.
     * Runs every hour.
     */
    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void cleanupExpiredSessions() {
        try {
            int deleted = sessionRepository.deleteAllExpiredSessions(Instant.now());
            if (deleted > 0) {
                logger.info("Cleaned up {} expired sessions", deleted);
            }
        } catch (Exception e) {
            logger.error("Error cleaning up expired sessions", e);
        }
    }

    /**
     * Generates a unique session token.
     *
     * @return the generated token
     */
    private String generateSessionToken() {
        return UUID.randomUUID().toString();
    }
}

