package com.codebridge.server.sessions;

import com.codebridge.server.config.SshSessionConfigProperties;
import com.codebridge.server.dto.sessions.KeepAliveResponse;
import com.codebridge.server.dto.sessions.SessionResponse;
import com.codebridge.server.exception.ResourceNotFoundException;
import com.codebridge.server.model.Server;
import com.codebridge.server.model.SshKey;
import com.codebridge.server.model.enums.ServerAuthProvider;
import com.codebridge.server.security.jwt.JwtTokenProvider;
import com.codebridge.server.service.ServerAccessControlService;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import io.jsonwebtoken.Jwt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

@Service
public class InMemorySessionManagerImpl implements SessionManager<SshSessionWrapper> {

    private static final Logger log = LoggerFactory.getLogger(InMemorySessionManagerImpl.class);
    private final ConcurrentMap<SessionKey, SshSessionWrapper> activeSessions = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, SessionKey> sessionTokenToKeyMap = new ConcurrentHashMap<>();

    private final SshSessionConfigProperties configProperties;
    private final ServerAccessControlService serverAccessControlService;
    private final JwtTokenProvider jwtTokenProvider; // Added for JWT handling

    // Connect timeout for JSch session establishment within initSshSession factory
    private static final int CONNECT_TIMEOUT_MS = 20000;


    @Autowired
    public InMemorySessionManagerImpl(SshSessionConfigProperties configProperties,
                                      ServerAccessControlService serverAccessControlService,
                                      JwtTokenProvider jwtTokenProvider) {
        this.configProperties = configProperties;
        this.serverAccessControlService = serverAccessControlService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public Optional<SshSessionWrapper> getSession(SessionKey key) {
        if (key == null) {
            log.warn("Attempted to get session with null key.");
            return Optional.empty();
        }
        SshSessionWrapper wrapper = activeSessions.get(key);

        if (wrapper == null) {
            log.debug("No active session found for key: {}", key);
            return Optional.empty();
        }

        if (!wrapper.isConnected()) {
            log.warn("Session found for key {} but was not connected. Releasing it.", key);
            releaseSession(key); // Clean up the disconnected session
            return Optional.empty();
        }

        wrapper.updateLastAccessedTime();
        // storeSession(key, wrapper); // No need to call storeSession if only lastAccessedTime is updated in place.
                                    // If SshSessionWrapper were immutable and updateLastAccessedTime returned a new instance,
                                    // then storeSession would be necessary.
                                    // For mutable SshSessionWrapper, direct update is fine and simpler.
        log.debug("Retrieved and updated last access time for active session with key: {}", key);
        return Optional.of(wrapper);
    }

    @Override
    public SshSessionWrapper createSession(SessionKey key, Supplier<SshSessionWrapper> sessionFactory) throws Exception {
        if (key == null) {
            log.error("Attempted to create session with null key.");
            throw new IllegalArgumentException("SessionKey cannot be null for createSession.");
        }
        if (sessionFactory == null) {
            log.error("Attempted to create session with null factory for key: {}", key);
            throw new IllegalArgumentException("SessionFactory cannot be null for createSession.");
        }

        log.info("Attempting to create new SSH session for key: {}", key);
        // Evict any existing session for this key before creating a new one to prevent resource leaks
        // if the previous session was not properly cleaned up.
        if (activeSessions.containsKey(key)) {
            log.warn("Existing session found for key {} during create operation. Releasing old session first.", key);
            releaseSession(key);
        }

        SshSessionWrapper newWrapper;
        try {
            newWrapper = sessionFactory.get();
        } catch (Exception e) {
            log.error("Session factory failed to create session for key: {}", key, e);
            throw e; // Rethrow the original exception
        }

        if (newWrapper == null) {
            log.error("Session factory returned null for key: {}. Cannot store session.", key);
            throw new RuntimeException("Session factory returned null for key: " + key);
        }

        if (!newWrapper.isConnected()) {
            log.error("Session factory created a session for key: {} but it is not connected. Discarding.", key);
            // Attempt to disconnect to free resources if any were partially acquired
            newWrapper.disconnect();
            throw new RuntimeException("Session factory created a disconnected session for key: " + key);
        }

        activeSessions.put(key, newWrapper);
        log.info("Successfully created and cached SSH session for key: {}", key);
        return newWrapper;
    }

    @Override
    public void storeSession(SessionKey key, SshSessionWrapper sessionWrapper) {
        if (key == null || sessionWrapper == null) {
            log.warn("Attempted to store session with null key or wrapper. Key: {}, Wrapper: {}", key, sessionWrapper);
            return;
        }
        activeSessions.put(key, sessionWrapper);
        log.debug("Stored/Updated session for key: {}", key);
    }

    @Override
    public boolean releaseSession(SessionKey key) {
        if (key == null) {
            log.warn("Attempted to release session with null key.");
            return false;
        }
        SshSessionWrapper wrapper = activeSessions.remove(key);

        if (wrapper != null) {
            log.info("Releasing session for key: {}", key);
            wrapper.disconnect(); // Ensure underlying JSch session is closed
            log.info("Session for key: {} released and disconnected.", key);
            return true;
        } else {
            log.debug("No session found to release for key: {}", key);
            return false;
        }
    }

    @Override
    public void cleanupExpiredSessions() {
        log.debug("Starting expired session cleanup task...");
        long timeoutMs = configProperties.getTimeoutMs();
        long currentTime = System.currentTimeMillis();
        int expiredSessionCount = 0;

        Iterator<Map.Entry<SessionKey, SshSessionWrapper>> activeSessionsIterator = activeSessions.entrySet().iterator();
        while (activeSessionsIterator.hasNext()) {
            Map.Entry<SessionKey, SshSessionWrapper> entry = activeSessionsIterator.next();
            SessionKey key = entry.getKey();
            SshSessionWrapper wrapper = entry.getValue();

            if ((currentTime - wrapper.getLastAccessedTime()) > timeoutMs) {
                log.info("SSH session for key: {} has expired (last accessed: {}ms ago). Disconnecting and removing.",
                         key, (currentTime - wrapper.getLastAccessedTime()));
                try {
                    wrapper.disconnect();
                } catch (Exception e) {
                    log.error("Error while disconnecting expired session for key: {}: {}", key, e.getMessage(), e);
                }
                activeSessionsIterator.remove(); // Remove from activeSessions
                expiredSessionCount++;

                // Also remove any associated tokens from sessionTokenToKeyMap
                sessionTokenToKeyMap.entrySet().removeIf(tokenEntry -> tokenEntry.getValue().equals(key));
            }
        }

        if (expiredSessionCount > 0) {
            log.info("Expired session cleanup task completed. Removed {} expired SSH sessions and associated tokens.", expiredSessionCount);
        } else {
            log.debug("Expired session cleanup task completed. No SSH sessions were expired.");
        }
    }

    // New methods for explicit session management via API-like calls

    public SessionResponse initSshSession(UUID platformUserId, UUID serverId) throws RuntimeException {
        SessionKey sessionKey = new SessionKey(platformUserId, serverId, "SSH");

        // Prevent re-initialization if a session for this key already exists and is active
        // Or, define policy: allow re-init to get a new token and refresh session?
        // For now, if active, re-use is not implicitly done by init, it creates a new one or fails if already there.
        // The createSession method already handles eviction if a session for the key exists.

        final ServerAccessControlService.UserSpecificConnectionDetails details;
        try {
            details = serverAccessControlService.checkUserAccessAndGetConnectionDetails(platformUserId, serverId);
        } catch (ResourceNotFoundException | com.codebridge.server.exception.AccessDeniedException e) {
            log.error("Access denied or server not found for user {}, server {}: {}", platformUserId, serverId, e.getMessage());
            throw new RuntimeException("Session initialization failed: Access denied or server not found.", e);
        }

        Server server = details.server();
        SshKey sshKey = details.decryptedSshKey();

        if (server.getAuthProvider() != ServerAuthProvider.SSH_KEY) {
            throw new RuntimeException("Session initialization failed: Server " + serverId + " is not configured for SSH Key authentication.");
        }
        if (sshKey == null || sshKey.getPrivateKeyBytes() == null || sshKey.getPrivateKeyBytes().length == 0) {
            throw new RuntimeException("Session initialization failed: Private key is missing or empty for server " + serverId);
        }

        Supplier<SshSessionWrapper> sshSessionFactory = () -> {
            try {
                JSch jsch = new JSch();
                jsch.addIdentity(
                    "sshKey_" + sshKey.getId().toString(),
                    sshKey.getPrivateKeyBytes(),
                    sshKey.getPublicKeyBytes(),
                    null // No passphrase for now
                );
                com.jcraft.jsch.Session newJschSession = jsch.getSession(
                    details.remoteUsername(),
                    server.getHostname(),
                    server.getPort()
                );
                newJschSession.setConfig("StrictHostKeyChecking", "no"); // TODO: Production hardening
                newJschSession.setTimeout(CONNECT_TIMEOUT_MS); // Use class constant
                log.info("Attempting JSch session connect for init (key: {})", sessionKey);
                newJschSession.connect();
                log.info("JSch session connected for init (key: {})", sessionKey);
                return new SshSessionWrapper(sessionKey, newJschSession);
            } catch (JSchException e) {
                log.error("JSchException during new SSH session creation for key {}: {}", sessionKey, e.getMessage());
                throw new RuntimeException("Failed to create SSH session: " + e.getMessage(), e);
            }
        };

        try {
            // this.createSession will store it in activeSessions
            /* SshSessionWrapper sessionWrapper = */ this.createSession(sessionKey, sshSessionFactory);

            String sessionToken = jwtTokenProvider.generateToken(sessionKey); // Use JWT
            sessionTokenToKeyMap.put(sessionToken, sessionKey);
            log.info("Initialized and cached SSH session for key: {}, JWT token generated.", sessionKey);

            // expiresIn should ideally come from JWT config if SessionResponse reflects token validity
            long expiresIn = jwtConfigProperties.getExpirationMs();
            return new SessionResponse(
                sessionToken,
                expiresIn,
                platformUserId,
                serverId,
                "SSH"
            );
        } catch (Exception e) { // Catch exceptions from createSession (which includes factory exceptions)
            log.error("Failed to initialize SSH session for user {} server {}: {}", platformUserId, serverId, e.getMessage(), e);
            throw new RuntimeException("Session initialization failed: " + e.getMessage(), e);
        }
    }

    public Optional<KeepAliveResponse> keepAliveSshSession(String sessionToken) {
        Optional<SessionKey> keyOpt = jwtTokenProvider.validateTokenAndExtractSessionKey(sessionToken);
        if (keyOpt.isEmpty()) {
            log.warn("Keepalive failed: Invalid or expired JWT token: {}", sessionToken);
            // Attempt to remove from map just in case it's there with an invalid/expired JWT
            sessionTokenToKeyMap.remove(sessionToken);
            return Optional.empty();
        }

        SessionKey key = keyOpt.get();
        Optional<SshSessionWrapper> wrapperOpt = this.getSession(key); // getSession updates lastAccessedTime

        if (wrapperOpt.isPresent()) {
            // Generate a new JWT to refresh the token validity
            String newSessionToken = jwtTokenProvider.generateToken(key);
            sessionTokenToKeyMap.remove(sessionToken); // Remove old token
            sessionTokenToKeyMap.put(newSessionToken, key); // Add new token

            log.info("Keepalive successful for session key: {}. New JWT token generated.", key);
            return Optional.of(new KeepAliveResponse(newSessionToken, jwtConfigProperties.getExpirationMs()));
        } else {
            log.warn("Keepalive failed: Session not active or expired for key: {} (derived from token {}). Cleaning up token.", key, sessionToken);
            sessionTokenToKeyMap.remove(sessionToken); // Clean up dangling token
            return Optional.empty();
        }
    }

    public void releaseSshSession(String sessionToken) {
        Optional<SessionKey> keyOpt = jwtTokenProvider.validateTokenAndExtractSessionKey(sessionToken);
        if (keyOpt.isEmpty()) {
            log.warn("Release failed: Invalid or expired JWT token: {}", sessionToken);
            // Attempt to remove from map just in case an invalid/expired JWT was somehow stored
            sessionTokenToKeyMap.remove(sessionToken);
            return;
        }

        SessionKey key = keyOpt.get();
        // Remove token from map first, regardless of whether the session is still active.
        // This ensures the token cannot be reused.
        sessionTokenToKeyMap.remove(sessionToken);
        log.info("Attempting to release SSH session for key: {} (derived from token {})", key, sessionToken);

        boolean released = this.releaseSession(key); // This disconnects and removes from activeSessions
        if (released) {
            log.info("Successfully released SSH session for key: {}", key);
        } else {
            log.warn("Session key {} (derived from token {}) was not found in active sessions for release.", key, sessionToken);
        }
    }
}
