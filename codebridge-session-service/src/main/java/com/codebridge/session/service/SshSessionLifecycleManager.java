package com.codebridge.session.service;

import com.codebridge.session.config.ApplicationInstanceIdProvider;
import com.codebridge.session.config.JwtConfigProperties; // For session token generation
import com.codebridge.session.config.SshSessionConfigProperties;
import com.codebridge.session.dto.KeepAliveResponse;
import com.codebridge.session.dto.SessionResponse;
import com.codebridge.session.dto.SshSessionMetadata;
import com.codebridge.session.dto.UserProvidedConnectionDetails;
import com.codebridge.session.exception.RemoteOperationException;
import com.codebridge.session.exception.ResourceNotFoundException;
import com.codebridge.session.model.SessionKey;
import com.codebridge.session.model.SshSessionWrapper;
import com.codebridge.session.model.enums.ServerAuthProvider; // Ensure this path is correct
import com.codebridge.session.security.jwt.JwtTokenProvider;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service("sshSessionLifecycleManager") // Explicit name
public class SshSessionLifecycleManager {

    private static final Logger logger = LoggerFactory.getLogger(SshSessionLifecycleManager.class);
    private static final String SSH_SESSION_TYPE = "SSH";
    private static final int JSCH_CONNECT_TIMEOUT_MS = 15000; // 15 seconds

    private final RedisTemplate<String, SessionKey> jwtToSessionKeyRedisTemplate;
    private final RedisTemplate<String, SshSessionMetadata> sessionMetadataRedisTemplate;
    private final JwtTokenProvider jwtTokenProvider; // For session tokens issued by this service
    private final SshSessionConfigProperties sshConfig;
    private final JwtConfigProperties jwtConfig; // For session token expiry
    private final ApplicationInstanceIdProvider instanceIdProvider;
    private final CustomJschHostKeyRepository customJschHostKeyRepository; // Added
    private final String applicationInstanceId;

    private final ConcurrentHashMap<SessionKey, SshSessionWrapper> localActiveSshSessions = new ConcurrentHashMap<>();

    public SshSessionLifecycleManager(
            RedisTemplate<String, SessionKey> jwtToSessionKeyRedisTemplate,
            RedisTemplate<String, SshSessionMetadata> sessionMetadataRedisTemplate,
            JwtTokenProvider jwtTokenProvider,
            SshSessionConfigProperties sshConfig,
            JwtConfigProperties jwtConfig, // Used for session token expiry
            ApplicationInstanceIdProvider instanceIdProvider,
            CustomJschHostKeyRepository customJschHostKeyRepository) { // Added
        this.jwtToSessionKeyRedisTemplate = jwtToSessionKeyRedisTemplate;
        this.sessionMetadataRedisTemplate = sessionMetadataRedisTemplate;
        this.jwtTokenProvider = jwtTokenProvider;
        this.sshConfig = sshConfig;
        this.jwtConfig = jwtConfig;
        this.instanceIdProvider = instanceIdProvider;
        this.customJschHostKeyRepository = customJschHostKeyRepository; // Added
        this.applicationInstanceId = this.instanceIdProvider.getInstanceId();
    }

    // --- Redis Key Helpers ---
    private String sshTokenRedisKey(String sessionToken) {
        return "session:ssh:token:" + sessionToken;
    }

    private String sshSessionMetadataRedisKey(SessionKey sessionKey) {
        return "session:ssh:metadata:" + sessionKey.platformUserId() + ":" + sessionKey.resourceId() + ":" + sessionKey.sessionType();
    }
    
    // --- Public API Methods ---

    public SessionResponse initSshSession(UUID platformUserId, UUID serverId, UserProvidedConnectionDetails connDetails) {
        SessionKey sessionKey = new SessionKey(platformUserId, serverId, SSH_SESSION_TYPE);
        logger.info("Initializing SSH session for key: {}", sessionKey);

        // Optional: Limit max active sessions per user/server (check metadata count in Redis or local cache size)
        // if (localActiveSshSessions.size() >= sshConfig.getMaxSessionsPerUserPerServer()) { // This is instance local, not global
        //    throw new RemoteOperationException("Max active SSH sessions reached for this user/server on this instance.");
        // }
        
        forceReleaseSshSessionByKey(sessionKey, false); // Clean up any stale session for this exact key

        Session jschSession;
        try {
            jschSession = createJschSession(connDetails);
            jschSession.connect(JSCH_CONNECT_TIMEOUT_MS);
            logger.info("JSch session connected for {}", sessionKey);
        } catch (JSchException e) {
            logger.error("JSchException during SSH connection for {}: {}", sessionKey, e.getMessage(), e);
            throw new RemoteOperationException("SSH connection failed: " + e.getMessage(), e);
        }

        SshSessionWrapper wrapper = new SshSessionWrapper(jschSession, sessionKey);
        localActiveSshSessions.put(sessionKey, wrapper);

        String sessionToken = jwtTokenProvider.generateToken(sessionKey, platformUserId);
        long currentTime = Instant.now().toEpochMilli();
        long expiresAt = currentTime + jwtConfig.getExpirationMs();

        SshSessionMetadata metadata = new SshSessionMetadata(
                platformUserId, serverId, sessionToken, currentTime, currentTime, expiresAt, applicationInstanceId);
        
        jwtToSessionKeyRedisTemplate.opsForValue().set(sshTokenRedisKey(sessionToken), sessionKey, jwtConfig.getExpirationMs(), TimeUnit.MILLISECONDS);
        sessionMetadataRedisTemplate.opsForValue().set(sshSessionMetadataRedisKey(sessionKey), metadata, jwtConfig.getExpirationMs(), TimeUnit.MILLISECONDS);
        
        logger.info("SSH session initialized successfully for {}. Token: {}", sessionKey, sessionToken);
        return new SessionResponse(sessionToken, SSH_SESSION_TYPE, "ACTIVE", currentTime, expiresAt);
    }

    public KeepAliveResponse keepAliveSshSession(String sessionToken) {
        Claims claims = jwtTokenProvider.getClaimsFromToken(sessionToken);
        if (claims == null) {
            throw new RemoteOperationException("Invalid session token for keepalive.");
        }
        SessionKey sessionKey = jwtToSessionKeyRedisTemplate.opsForValue().get(sshTokenRedisKey(sessionToken));
        if (sessionKey == null) {
            throw new ResourceNotFoundException("Session not found or expired for keepalive (token mapping missing). Token: " + sessionToken);
        }

        SshSessionWrapper wrapper = localActiveSshSessions.get(sessionKey);
        if (wrapper == null || !wrapper.isConnected()) {
            // Session might be managed by another instance or locally expired
            // Attempt to fetch metadata to see if it's active elsewhere
            SshSessionMetadata metadata = sessionMetadataRedisTemplate.opsForValue().get(sshSessionMetadataRedisKey(sessionKey));
            if (metadata == null || metadata.expiresAt() < Instant.now().toEpochMilli()) {
                 forceReleaseSshSessionByKey(sessionKey, true); // Clean up if metadata confirms expiry
                 throw new ResourceNotFoundException("Session expired or not found in metadata. Token: " + sessionToken);
            }
            // If metadata exists and is valid, but not local, this instance can't directly keep JSch session alive
            // However, we can update the metadata's expiry and access time, and issue a new token.
            // The client will continue using the new token. If it hits the original instance, that one will keep JSch alive.
            // If it hits this instance again, this logic repeats. This is part of the hybrid model.
             logger.warn("Keepalive for session {} not local to this instance {}. Updating metadata only.", sessionKey, applicationInstanceId);
        } else {
             wrapper.updateLastAccessedTime(); // Update local access time
        }
        
        // Regardless of local presence, refresh token and metadata in Redis
        String newSessionToken = jwtTokenProvider.generateToken(sessionKey, sessionKey.platformUserId());
        long currentTime = Instant.now().toEpochMilli();
        long newExpiresAt = currentTime + jwtConfig.getExpirationMs();

        SshSessionMetadata newMetadata = new SshSessionMetadata(
            sessionKey.platformUserId(), sessionKey.resourceId(), newSessionToken, 
            (wrapper != null ? wrapper.getSessionKey().hashCode() : currentTime), // Use original creation if available, else current for metadata creation time
            currentTime, newExpiresAt, applicationInstanceId // This instance claims it now
        );
        
        // Update Redis: new token maps to key, metadata updated with new token and expiry
        jwtToSessionKeyRedisTemplate.opsForValue().set(sshTokenRedisKey(newSessionToken), sessionKey, jwtConfig.getExpirationMs(), TimeUnit.MILLISECONDS);
        sessionMetadataRedisTemplate.opsForValue().set(sshSessionMetadataRedisKey(sessionKey), newMetadata, jwtConfig.getExpirationMs(), TimeUnit.MILLISECONDS);
        // Delete old token mapping if it's different (it will be)
        if (!sessionToken.equals(newSessionToken)) {
            jwtToSessionKeyRedisTemplate.delete(sshTokenRedisKey(sessionToken));
        }

        logger.info("SSH session keepalive successful for {}. New token issued.", sessionKey);
        return new KeepAliveResponse(newSessionToken, "ACTIVE", newExpiresAt);
    }
    
    public void releaseSshSession(String sessionToken) {
        Claims claims = jwtTokenProvider.getClaimsFromToken(sessionToken);
        if (claims == null) {
            logger.warn("Attempted to release session with invalid token.");
            return; // Or throw
        }
        SessionKey sessionKey = jwtToSessionKeyRedisTemplate.opsForValue().get(sshTokenRedisKey(sessionToken));
        if (sessionKey == null) {
            logger.warn("No active session found for token to release: {}", sessionToken);
            return; // Session already gone or token invalid
        }
        forceReleaseSshSessionByKey(sessionKey, true);
        logger.info("SSH session released for key {} by token.", sessionKey);
    }

    public void forceReleaseSshSessionByKey(SessionKey key, boolean wasTokenBasedRelease) {
        logger.debug("Forcing release for session key: {}. Token-based: {}", key, wasTokenBasedRelease);
        SshSessionWrapper wrapper = localActiveSshSessions.remove(key);
        if (wrapper != null) {
            wrapper.disconnect();
            logger.info("Disconnected local JSch session for key: {}", key);
        }

        // If not token-based, we need to find the token from metadata to delete token mapping
        String tokenToDelete = null;
        SshSessionMetadata metadata = sessionMetadataRedisTemplate.opsForValue().get(sshSessionMetadataRedisKey(key));
        if (metadata != null) {
            tokenToDelete = metadata.sessionToken();
            sessionMetadataRedisTemplate.delete(sshSessionMetadataRedisKey(key));
            logger.debug("Deleted session metadata from Redis for key: {}", key);
        }
        
        if (wasTokenBasedRelease) {
            // The token used for release is implicitly handled if it's the one in metadata.
            // If it was a different (older) token for the same sessionKey, this logic is fine.
            // The calling method `releaseSshSession` would have used the specific token.
            // Here, if a valid token was in metadata, we ensure its mapping is removed.
            if (tokenToDelete != null) {
                 jwtToSessionKeyRedisTemplate.delete(sshTokenRedisKey(tokenToDelete));
                 logger.debug("Deleted token-to-key mapping from Redis for token: {}", tokenToDelete);
            }
        } else if (tokenToDelete != null) {
            // If not token based (e.g. internal cleanup or new init), use token from metadata
            jwtToSessionKeyRedisTemplate.delete(sshTokenRedisKey(tokenToDelete));
            logger.debug("Deleted token-to-key mapping from Redis (via metadata) for key: {}", key);
        }
    }

    @Scheduled(fixedDelayString = "${codebridge.session.ssh.defaultTimeoutMs:300000}", initialDelayString = "60000") // Check more frequently than timeout
    public void cleanupExpiredSshSessions() {
        logger.info("Running scheduled cleanup of expired SSH sessions on instance {}", applicationInstanceId);
        long now = Instant.now().toEpochMilli();
        long sessionTimeoutMs = sshConfig.getDefaultTimeoutMs();

        localActiveSshSessions.forEach((key, wrapper) -> {
            if (!wrapper.isConnected()) {
                logger.info("Local session for key {} is not connected. Removing from local cache.", key);
                localActiveSshSessions.remove(key); // Remove if JSch session died for other reasons
                // Consider also cleaning Redis if this instance was authoritative, but metadata might be more up-to-date
            } else if (now - wrapper.getLastAccessedTime() > sessionTimeoutMs) {
                logger.info("Local SSH session for key {} expired ({} ms idle). Releasing.", key, now - wrapper.getLastAccessedTime());
                forceReleaseSshSessionByKey(key, false); // Not token-based, so find token via metadata
            }
        });
        
        // Additional Redis-only cleanup for sessions potentially managed by other (dead) instances
        // This is more complex: requires iterating Redis keys (scan) or a different strategy (e.g. Redis TTLs being primary mechanism)
        // For now, local cleanup is the focus of this scheduled task. Redis TTLs on metadata and token keys handle Redis-side expiry.
    }
    
    // --- Helper Methods ---
    private Session createJschSession(UserProvidedConnectionDetails connDetails) throws JSchException {
        JSch jsch = new JSch();
        jsch.setHostKeyRepository(customJschHostKeyRepository); // Use custom HostKeyRepository

        if (connDetails.getAuthProvider() == ServerAuthProvider.SSH_KEY) {
            if (!StringUtils.hasText(connDetails.getDecryptedPrivateKey())) {
                throw new JSchException("Private key is required for SSH key authentication but was not provided.");
            }
            String keyName = StringUtils.hasText(connDetails.getSshKeyName()) ? connDetails.getSshKeyName() : "user-key";
            jsch.addIdentity(keyName, connDetails.getDecryptedPrivateKey().getBytes(), null, null); // Assuming no passphrase for key
        }

        Session session = jsch.getSession(connDetails.getUsername(), connDetails.getHostname(), connDetails.getPort());
        if (connDetails.getAuthProvider() == ServerAuthProvider.PASSWORD) {
             if (!StringUtils.hasText(connDetails.getDecryptedPassword())) {
                throw new JSchException("Password is required for password authentication but was not provided.");
            }
            session.setPassword(connDetails.getDecryptedPassword());
        }
        
        java.util.Properties config = new java.util.Properties();
        // config.put("StrictHostKeyChecking", "no"); // Removed, handled by CustomJschHostKeyRepository
        config.put("PreferredAuthentications", "publickey,password"); // Allow both, JSch will try based on what's available
        session.setConfig(config);
        return session;
    }

    // --- Getters for internal components (e.g., for operational services if they run on same instance) ---
    public SshSessionWrapper getLocalSession(SessionKey key) {
        return localActiveSshSessions.get(key);
    }

    public SshSessionMetadata getSessionMetadata(SessionKey key) {
        return sessionMetadataRedisTemplate.opsForValue().get(sshSessionMetadataRedisKey(key));
    }
    
    // This method is crucial for the hybrid model when an operation happens on an instance
    // that holds the live JSch session.
    public void updateSessionAccessTime(SessionKey key, SshSessionWrapper wrapper) {
        if (wrapper == null || !wrapper.isConnected()) return;

        wrapper.updateLastAccessedTime(); // Update local
        
        // Update Redis metadata, including setting this instance as the host
        SshSessionMetadata currentMetadata = sessionMetadataRedisTemplate.opsForValue().get(sshSessionMetadataRedisKey(key));
        if (currentMetadata != null) {
            SshSessionMetadata updatedMetadata = new SshSessionMetadata(
                currentMetadata.platformUserId(),
                currentMetadata.serverId(),
                currentMetadata.sessionToken(), // Keep the same token unless refreshed by keepalive
                currentMetadata.createdAt(),
                Instant.now().toEpochMilli(),
                currentMetadata.expiresAt(), // Expiry is managed by keepalive/token refresh
                this.applicationInstanceId // This instance is now the host
            );
            sessionMetadataRedisTemplate.opsForValue().set(sshSessionMetadataRedisKey(key), updatedMetadata, jwtConfig.getExpirationMs(), TimeUnit.MILLISECONDS);
        } else {
            logger.warn("No metadata found in Redis to update access time for session key: {}", key);
        }
    }
}
