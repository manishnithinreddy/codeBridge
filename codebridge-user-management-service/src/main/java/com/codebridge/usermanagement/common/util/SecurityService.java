package com.codebridge.usermanagement.common.util;

import com.codebridge.usermanagement.session.model.UserSession;
import com.codebridge.usermanagement.session.service.UserSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service for security-related operations.
 */
@Service
public class SecurityService {

    private final UserSessionService sessionService;

    @Autowired
    public SecurityService(UserSessionService sessionService) {
        this.sessionService = sessionService;
    }

    /**
     * Get the current user ID.
     *
     * @return The user ID
     */
    public UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return UUID.fromString(authentication.getName());
        }
        return null;
    }

    /**
     * Check if the current user is the specified user.
     *
     * @param userId The user ID
     * @return True if the current user is the specified user, false otherwise
     */
    public boolean isCurrentUser(UUID userId) {
        UUID currentUserId = getCurrentUserId();
        return currentUserId != null && currentUserId.equals(userId);
    }

    /**
     * Check if the current user is the owner of the specified session.
     *
     * @param sessionId The session ID
     * @return True if the current user is the owner of the session, false otherwise
     */
    public boolean isSessionOwner(UUID sessionId) {
        UUID currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            return false;
        }

        UserSession session = sessionService.findById(sessionId);
        return session.getUserId().equals(currentUserId);
    }
}

