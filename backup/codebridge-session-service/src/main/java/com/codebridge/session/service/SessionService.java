package com.codebridge.session.service;

import com.codebridge.session.model.SessionKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing session state and validation
 */
@Service
public class SessionService {

    private final Map<String, Boolean> activeSessions = new ConcurrentHashMap<>();
    
    @Autowired
    private SshSessionLifecycleManager sshSessionLifecycleManager;
    
    /**
     * Check if a session is active
     * @param sessionId The session ID to check
     * @return true if the session is active, false otherwise
     */
    public boolean isSessionActive(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return false;
        }
        
        // First check our local cache
        Boolean isActive = activeSessions.get(sessionId);
        if (isActive != null) {
            return isActive;
        }
        
        // If not in cache, try to parse it as a UUID and check with SSH session manager
        try {
            UUID sessionUuid = UUID.fromString(sessionId);
            // This is a simplified check - in a real implementation, we would need to 
            // extract the platform user ID and server ID from the session ID
            boolean active = sshSessionLifecycleManager.hasAnySessionForUser(sessionUuid);
            activeSessions.put(sessionId, active);
            return active;
        } catch (IllegalArgumentException e) {
            // Not a valid UUID
            return false;
        }
    }
    
    /**
     * Mark a session as active
     * @param sessionId The session ID to mark as active
     */
    public void markSessionActive(String sessionId) {
        if (sessionId != null && !sessionId.isEmpty()) {
            activeSessions.put(sessionId, true);
        }
    }
    
    /**
     * Mark a session as inactive
     * @param sessionId The session ID to mark as inactive
     */
    public void markSessionInactive(String sessionId) {
        if (sessionId != null && !sessionId.isEmpty()) {
            activeSessions.remove(sessionId);
        }
    }
}

