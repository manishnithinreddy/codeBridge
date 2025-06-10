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
import com.codebridge.session.service.circuit.CircuitBreaker;
import com.codebridge.session.service.connection.SshConnectionPool;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import io.jsonwebtoken.Claims;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
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
    private final CustomJschHostKeyRepository customJschHostKeyRepository;
    private final String applicationInstanceId;

    private final SshConnectionPool connectionPool;
    private final MeterRegistry meterRegistry;
    private final CircuitBreaker<Session> connectionCircuitBreaker;

    private final ConcurrentHashMap<SessionKey, SshSessionWrapper> localActiveSshSessions = new ConcurrentHashMap<>();

    public SshSessionLifecycleManager(
            RedisTemplate<String, SessionKey> jwtToSessionKeyRedisTemplate,
            RedisTemplate<String, SshSessionMetadata> sessionMetadataRedisTemplate,
            JwtTokenProvider jwtTokenProvider,
            SshSessionConfigProperties sshConfig,
            JwtConfigProperties jwtConfig,
            ApplicationInstanceIdProvider instanceIdProvider,
            CustomJschHostKeyRepository customJschHostKeyRepository,
            SshConnectionPool connectionPool,
            MeterRegistry meterRegistry) {
        this.jwtToSessionKeyRedisTemplate = jwtToSessionKeyRedisTemplate;
        this.sessionMetadataRedisTemplate = sessionMetadataRedisTemplate;
        this.jwtTokenProvider = jwtTokenProvider;
        this.sshConfig = sshConfig;
        this.jwtConfig = jwtConfig;
        this.instanceIdProvider = instanceIdProvider;
        this.customJschHostKeyRepository = customJschHostKeyRepository;
        this.applicationInstanceId = this.instanceIdProvider.getInstanceId();
        
        this.connectionPool = connectionPool;
        this.meterRegistry = meterRegistry;
        
        this.connectionCircuitBreaker = new CircuitBreaker<>(
                "ssh-connection",
                5, // 5 consecutive failures will trip the circuit
                Duration.ofMinutes(1), // Try resetting after 1 minute
                meterRegistry
        );
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

        forceReleaseSshSessionByKey(sessionKey, false); // Clean up any stale session for this exact key

        Session jschSession;
        try {
            jschSession = connectionCircuitBreaker.execute(() -> {
                try {
                    return connectionPool.acquireConnection(sessionKey, connDetails);
                } catch (JSchException | InterruptedException e) {
                    throw new RemoteOperationException("Failed to acquire SSH connection: " + e.getMessage(), e);
                }
            });
            logger.info("JSch session connected for {}", sessionKey);
        } catch (Exception e) {
            logger.error("Error during SSH connection for {}: {}", sessionKey, e.getMessage(), e);
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
            logger.warn("Keepalive for session {} not local to this instance {}. Updating metadata only.", sessionKey, applicationInstanceId);
        } else {
            wrapper.updateLastAccessedTime(); // Update local access time
        }

        String newSessionToken = jwtTokenProvider.generateToken(sessionKey, sessionKey.platformUserId());
        long currentTime = Instant.now().toEpochMilli();
        long newExpiresAt = currentTime + jwtConfig.getExpirationMs();

        SshSessionMetadata newMetadata = new SshSessionMetadata(
            sessionKey.platformUserId(), sessionKey.resourceId(), newSessionToken,
            (wrapper != null ? wrapper.getSessionKey().hashCode() : currentTime), // Use original creation if available, else current for metadata creation time
            currentTime, newExpiresAt, applicationInstanceId // This instance claims it now
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
            connectionPool.releaseConnection(key, true);
            logger.info("Released SSH connection back to pool for key: {}", key);
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
        } else if (tokenToDelete != null) {
            jwtToSessionKeyRedisTemplate.delete(sshTokenRedisKey(tokenToDelete));
            logger.debug("Deleted token-to-key mapping from Redis (via metadata) for key: {}", key);
        }
    }

    @Scheduled(fixedDelayString = "${codebridge.session.ssh.cleanupIntervalMs:60000}", initialDelayString = "60000")
    public void cleanupExpiredSshSessions() {
        logger.info("Running scheduled cleanup of expired SSH sessions on instance {}", applicationInstanceId);
        
        connectionPool.cleanupIdleConnections();
        
        long now = Instant.now().toEpochMilli();
        long sessionTimeoutMs = sshConfig.getDefaultTimeoutMs();

        localActiveSshSessions.forEach((key, wrapper) -> {
            if (!wrapper.isConnected()) {
                logger.info("Local session for key {} is not connected. Removing from local cache.", key);
                localActiveSshSessions.remove(key);
            } else if (now - wrapper.getLastAccessedTime() > sessionTimeoutMs) {
                logger.info("Local SSH session for key {} expired ({} ms idle). Releasing.", key, now - wrapper.getLastAccessedTime());
                forceReleaseSshSessionByKey(key, false);
            }
        });
    }

    public SshSessionWrapper getLocalSession(SessionKey key) {
        return localActiveSshSessions.get(key);
    }

    public SshSessionMetadata getSessionMetadata(SessionKey key) {
        return sessionMetadataRedisTemplate.opsForValue().get(sshSessionMetadataRedisKey(key));
    }

    public void updateSessionAccessTime(SessionKey key, SshSessionWrapper wrapper) {
        if (wrapper == null || !wrapper.isConnected()) return;

        wrapper.updateLastAccessedTime(); // Update local

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

    public boolean hasAnySessionForUser(UUID platformUserId) {
        if (platformUserId == null) {
            return false;
        }
        
        // Check local sessions first
        for (Map.Entry<SessionKey, SshSessionWrapper> entry : localActiveSshSessions.entrySet()) {
            if (entry.getKey().platformUserId().equals(platformUserId) && entry.getValue().isConnected()) {
                return true;
            }
        }
        
        // Check Redis for distributed sessions
        String keyPattern = "session:metadata:" + platformUserId + ":*";
        return !sessionMetadataRedisTemplate.keys(keyPattern).isEmpty();
    }

    public SshConnectionPool getConnectionPool() {
        return connectionPool;
    }

    public CircuitBreaker<Session> getConnectionCircuitBreaker() {
        return connectionCircuitBreaker;
    }
}
