package com.codebridge.usermanagement.session.service;

import com.codebridge.usermanagement.common.exception.ResourceNotFoundException;
import com.codebridge.usermanagement.session.model.UserSession;
import com.codebridge.usermanagement.session.repository.UserSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for user session operations.
 */
@Service
public class UserSessionService {

    private static final Logger logger = LoggerFactory.getLogger(UserSessionService.class);

    private final UserSessionRepository sessionRepository;

    @Value("${session.expiration}")
    private long sessionExpirationMinutes;

    @Autowired
    public UserSessionService(UserSessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    /**
     * Create a new session.
     *
     * @param userId The user ID
     * @param token The token
     * @param ipAddress The IP address
     * @param userAgent The user agent
     * @return The session
     */
    @Transactional
    public UserSession createSession(UUID userId, String token, String ipAddress, String userAgent) {
        UserSession session = new UserSession();
        session.setUserId(userId);
        session.setToken(token);
        session.setIpAddress(ipAddress);
        session.setUserAgent(userAgent);
        session.setExpiresAt(LocalDateTime.now().plusMinutes(sessionExpirationMinutes));
        session.setLastAccessedAt(LocalDateTime.now());
        
        return sessionRepository.save(session);
    }

    /**
     * Find a session by ID.
     *
     * @param id The session ID
     * @return The session
     */
    public UserSession findById(UUID id) {
        return sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
    }

    /**
     * Find a session by token.
     *
     * @param token The token
     * @return The session
     */
    public Optional<UserSession> findByToken(String token) {
        return sessionRepository.findByToken(token);
    }

    /**
     * Find all active sessions for a user.
     *
     * @param userId The user ID
     * @return The sessions
     */
    public List<UserSession> findActiveSessionsByUserId(UUID userId) {
        return sessionRepository.findByUserIdAndActiveTrue(userId);
    }

    /**
     * Update a session.
     *
     * @param session The session
     * @return The updated session
     */
    @Transactional
    public UserSession updateSession(UserSession session) {
        return sessionRepository.save(session);
    }

    /**
     * Deactivate a session.
     *
     * @param id The session ID
     */
    @Transactional
    public void deactivateSession(UUID id) {
        UserSession session = findById(id);
        session.setActive(false);
        sessionRepository.save(session);
    }

    /**
     * Deactivate all sessions for a user.
     *
     * @param userId The user ID
     * @return The number of sessions deactivated
     */
    @Transactional
    public int deactivateAllUserSessions(UUID userId) {
        return sessionRepository.deactivateAllUserSessions(userId);
    }

    /**
     * Count active sessions for a user.
     *
     * @param userId The user ID
     * @return The number of active sessions
     */
    public long countActiveSessionsByUserId(UUID userId) {
        return sessionRepository.countByUserIdAndActiveTrue(userId);
    }

    /**
     * Clean up expired sessions.
     * Runs every hour.
     */
    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void cleanupExpiredSessions() {
        int count = sessionRepository.deactivateExpiredSessions(LocalDateTime.now());
        if (count > 0) {
            logger.info("Cleaned up {} expired sessions", count);
        }
    }
}

