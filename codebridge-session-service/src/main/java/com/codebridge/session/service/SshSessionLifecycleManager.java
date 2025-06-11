package com.codebridge.session.service;

import com.codebridge.session.config.ApplicationInstanceIdProvider;
import com.codebridge.session.config.JwtConfigProperties;
import com.codebridge.session.config.SshSessionConfigProperties;
import com.codebridge.session.dto.KeepAliveResponse;
import com.codebridge.session.dto.SessionResponse;
import com.codebridge.session.dto.SshSessionCredentials;
import com.codebridge.session.dto.SshSessionMetadata;
import com.codebridge.session.dto.UserProvidedConnectionDetails;
import com.codebridge.session.exception.RemoteOperationException;
import com.codebridge.session.exception.ResourceNotFoundException;
import com.codebridge.session.model.SessionKey;
import com.codebridge.session.model.SshSessionWrapper;
import com.codebridge.session.security.jwt.JwtTokenProvider;
import com.jcraft.jsch.JSchException;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service("sshSessionLifecycleManager")
public class SshSessionLifecycleManager {

    private static final Logger logger = LoggerFactory.getLogger(SshSessionLifecycleManager.class);
    private static final String SSH_SESSION_TYPE = "SSH";

    private final RedisTemplate<String, SessionKey> jwtToSessionKeyRedisTemplate;
    private final RedisTemplate<String, SshSessionMetadata> sessionMetadataRedisTemplate;
    private final JwtTokenProvider jwtTokenProvider;
    private final SshSessionConfigProperties sshConfig;
    private final JwtConfigProperties jwtConfig; // For session token expiry
    private final ApplicationInstanceIdProvider instanceIdProvider;
    private final String applicationInstanceId;

    // Make this package-private for testing
    final ConcurrentHashMap<SessionKey, SshSessionWrapper> localActiveSshSessions = new ConcurrentHashMap<>();

    public SshSessionLifecycleManager(
            RedisTemplate<String, SessionKey> jwtToSessionKeyRedisTemplate,
            RedisTemplate<String, SshSessionMetadata> sessionMetadataRedisTemplate,
            JwtTokenProvider jwtTokenProvider,
            SshSessionConfigProperties sshConfig,
            JwtConfigProperties jwtConfig,
            ApplicationInstanceIdProvider instanceIdProvider) {
        this.jwtToSessionKeyRedisTemplate = jwtToSessionKeyRedisTemplate;
        this.sessionMetadataRedisTemplate = sessionMetadataRedisTemplate;
        this.jwtTokenProvider = jwtTokenProvider;
        this.sshConfig = sshConfig;
        this.jwtConfig = jwtConfig;
        this.instanceIdProvider = instanceIdProvider;
        this.applicationInstanceId = this.instanceIdProvider.getInstanceId();
    }

    // --- Redis Key Helpers ---
    // Make these package-private for testing
    String sshTokenRedisKey(String sessionToken) {
        return "session:ssh:token:" + sessionToken;
    }

    String sshSessionMetadataRedisKey(SessionKey sessionKey) {
        return "session:ssh:metadata:" + sessionKey.platformUserId() + ":" + sessionKey.resourceId() + ":" + sessionKey.sessionType();
    }

    // --- Public API Methods ---

    public SessionResponse initSshSession(UUID platformUserId, UUID serverId, UserProvidedConnectionDetails connectionDetails) {
        SessionKey sessionKey = new SessionKey(platformUserId, serverId, SSH_SESSION_TYPE);
        logger.info("Initializing SSH session for key: {}", sessionKey);

        forceReleaseSshSessionByKey(sessionKey, false); // Clean up any stale session for this exact key

        // Convert UserProvidedConnectionDetails to SshSessionCredentials
        SshSessionCredentials credentials = new SshSessionCredentials();
        credentials.setHost(connectionDetails.getHostname());
        credentials.setPort(connectionDetails.getPort());
        credentials.setUsername(connectionDetails.getUsername());
        credentials.setPassword(connectionDetails.getDecryptedPassword());
        credentials.setPrivateKey(connectionDetails.getDecryptedPrivateKey());

        SshSessionWrapper wrapper;
        try {
            wrapper = new SshSessionWrapper(credentials.getHost(), credentials.getPort(), credentials.getUsername(), credentials.getPassword(), credentials.getPrivateKey());
            wrapper.connect();
            logger.info("SSH Connection established for {}", sessionKey);
        } catch (JSchException e) {
            logger.error("SSH Connection failed for {}: {}", sessionKey, e.getMessage(), e);
            throw new RemoteOperationException("SSH connection failed: " + e.getMessage(), e);
        }

        localActiveSshSessions.put(sessionKey, wrapper);

        String sessionToken = jwtTokenProvider.generateToken(sessionKey, platformUserId);
        long currentTime = Instant.now().toEpochMilli();
        long expiresAt = currentTime + jwtConfig.getExpirationMs(); // Use JWT general expiration

        SshSessionMetadata metadata = new SshSessionMetadata(
                platformUserId, serverId, sessionToken, currentTime, currentTime, expiresAt,
                applicationInstanceId, credentials.getHost(), credentials.getUsername()
        );

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
        if (!SSH_SESSION_TYPE.equals(sessionKey.sessionType())) {
            throw new RemoteOperationException("Invalid session type for SSH keepalive.");
        }

        SshSessionWrapper wrapper = localActiveSshSessions.get(sessionKey);
        if (wrapper == null || !wrapper.isConnected()) {
            SshSessionMetadata metadata = sessionMetadataRedisTemplate.opsForValue().get(sshSessionMetadataRedisKey(sessionKey));
            if (metadata == null || metadata.expiresAt() < Instant.now().toEpochMilli()) {
                 forceReleaseSshSessionByKey(sessionKey, true); // Clean up if metadata confirms expiry
                 throw new ResourceNotFoundException("Session expired or not found in metadata. Token: " + sessionToken);
            }
            // If metadata exists and is valid, but not local, this instance can't directly keep JSch session alive
            logger.warn("Keepalive for SSH session {} not local to this instance {}. Updating metadata only.", sessionKey, applicationInstanceId);
        } else {
            wrapper.updateLastAccessedTime();
        }

        String newSessionToken = jwtTokenProvider.generateToken(sessionKey, sessionKey.platformUserId());
        long currentTime = Instant.now().toEpochMilli();
        long newExpiresAt = currentTime + jwtConfig.getExpirationMs();

        SshSessionMetadata currentMetadata = sessionMetadataRedisTemplate.opsForValue().get(sshSessionMetadataRedisKey(sessionKey));
        String sshHost = currentMetadata != null ? currentMetadata.sshHost() : "";
        String sshUsername = currentMetadata != null ? currentMetadata.sshUsername() : "";

        SshSessionMetadata newMetadata = new SshSessionMetadata(
            sessionKey.platformUserId(),
            sessionKey.resourceId(),
            newSessionToken,
            (currentMetadata != null ? currentMetadata.createdAt() : currentTime),
            currentTime, newExpiresAt, applicationInstanceId,
            sshHost, sshUsername
        );

        jwtToSessionKeyRedisTemplate.opsForValue().set(sshTokenRedisKey(newSessionToken), sessionKey, jwtConfig.getExpirationMs(), TimeUnit.MILLISECONDS);
        sessionMetadataRedisTemplate.opsForValue().set(sshSessionMetadataRedisKey(sessionKey), newMetadata, jwtConfig.getExpirationMs(), TimeUnit.MILLISECONDS);
        if (!sessionToken.equals(newSessionToken)) {
            jwtToSessionKeyRedisTemplate.delete(sshTokenRedisKey(sessionToken));
        }

        logger.info("SSH session keepalive successful for {}. New token issued.", sessionKey);
        return new KeepAliveResponse(newSessionToken, "ACTIVE", newExpiresAt);
    }

    public void releaseSshSession(String sessionToken) {
        Claims claims = jwtTokenProvider.getClaimsFromToken(sessionToken);
        if (claims == null) {
            logger.warn("Attempted to release SSH session with invalid token.");
            return;
        }
        SessionKey sessionKey = jwtToSessionKeyRedisTemplate.opsForValue().get(sshTokenRedisKey(sessionToken));
        if (sessionKey == null) {
            logger.warn("No active SSH session found for token to release: {}", sessionToken);
            return;
        }
        forceReleaseSshSessionByKey(sessionKey, true);
        logger.info("SSH session released for key {} by token.", sessionKey);
    }

    public void forceReleaseSshSessionByKey(SessionKey key, boolean wasTokenBasedRelease) {
        logger.debug("Forcing release for SSH session key: {}. Token-based: {}", key, wasTokenBasedRelease);
        SshSessionWrapper wrapper = localActiveSshSessions.remove(key);
        if (wrapper != null) {
            wrapper.disconnect();
            logger.info("Closed local SSH connection for key: {}", key);
        }

        String tokenToDelete = null;
        SshSessionMetadata metadata = sessionMetadataRedisTemplate.opsForValue().get(sshSessionMetadataRedisKey(key));
        if (metadata != null) {
            tokenToDelete = metadata.sessionToken();
            sessionMetadataRedisTemplate.delete(sshSessionMetadataRedisKey(key));
            logger.debug("Deleted session metadata from Redis for key: {}", key);
        }

        if (wasTokenBasedRelease) {
            if (tokenToDelete != null) {
                jwtToSessionKeyRedisTemplate.delete(sshTokenRedisKey(tokenToDelete));
                logger.debug("Deleted token-to-key mapping from Redis for token: {}", tokenToDelete);
            }
        }
    }

    @Scheduled(fixedDelayString = "${codebridge.session.ssh.defaultTimeoutMs:1800000}", initialDelayString = "60000")
    public void cleanupExpiredSshSessions() {
        logger.info("Running scheduled cleanup of expired SSH sessions on instance {}", applicationInstanceId);
        long now = Instant.now().toEpochMilli();
        long sessionTimeoutMs = sshConfig.getDefaultTimeoutMs();

        localActiveSshSessions.forEach((key, wrapper) -> {
            if (!wrapper.isConnected()) {
                logger.info("Local SSH session for key {} is disconnected. Removing from local cache and releasing.", key);
                forceReleaseSshSessionByKey(key, false);
            } else if (now - wrapper.getLastAccessedTime() > sessionTimeoutMs) {
                logger.info("Local SSH session for key {} expired ({} ms idle). Releasing.", key, now - wrapper.getLastAccessedTime());
                forceReleaseSshSessionByKey(key, false);
            }
        });
    }

    // --- Getters for internal components (e.g., for operational services) ---
    public Optional<SshSessionWrapper> getLocalSession(SessionKey key) {
        return Optional.ofNullable(localActiveSshSessions.get(key));
    }

    public Optional<SshSessionMetadata> getSessionMetadata(SessionKey key) {
        return Optional.ofNullable(sessionMetadataRedisTemplate.opsForValue().get(sshSessionMetadataRedisKey(key)));
    }

    public void updateSessionAccessTime(SessionKey key, SshSessionWrapper wrapper) {
        if (wrapper == null || !wrapper.isConnected()) return;

        wrapper.updateLastAccessedTime();

        SshSessionMetadata currentMetadata = sessionMetadataRedisTemplate.opsForValue().get(sshSessionMetadataRedisKey(key));
        if (currentMetadata != null) {
            SshSessionMetadata updatedMetadata = new SshSessionMetadata(
                currentMetadata.platformUserId(),
                currentMetadata.serverId(),
                currentMetadata.sessionToken(), // Keep the same token unless refreshed by keepalive
                currentMetadata.createdAt(),
                Instant.now().toEpochMilli(),
                currentMetadata.expiresAt(),
                this.applicationInstanceId,
                currentMetadata.sshHost(),
                currentMetadata.sshUsername()
            );
            sessionMetadataRedisTemplate.opsForValue().set(sshSessionMetadataRedisKey(key), updatedMetadata, jwtConfig.getExpirationMs(), TimeUnit.MILLISECONDS);
        } else {
            logger.warn("No metadata found in Redis to update access time for session key: {}", key);
        }
    }
    
    // Helper method to check if a user has any active sessions
    public boolean hasAnySessionForUser(UUID platformUserId) {
        // This is a simplified implementation - in a real system, you might query Redis
        // to check for any sessions belonging to this user
        return localActiveSshSessions.keySet().stream()
                .anyMatch(key -> key.platformUserId().equals(platformUserId));
    }
}

