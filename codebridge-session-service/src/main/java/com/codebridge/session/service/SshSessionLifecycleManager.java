package com.codebridge.session.service;

import com.codebridge.session.config.JwtConfigProperties;
import com.codebridge.session.config.SshSessionConfigProperties;
import com.codebridge.session.dto.KeepAliveResponse;
import com.codebridge.session.dto.SessionResponse;
import com.codebridge.session.dto.SshSessionMetadata;
import com.codebridge.session.dto.UserProvidedConnectionDetails;
import com.codebridge.session.model.SessionKey;
import com.codebridge.session.model.SshSessionWrapper;
import com.codebridge.session.security.jwt.JwtTokenProvider;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// import org.springframework.beans.factory.annotation.Value; // Replaced by ApplicationInstanceIdProvider
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Service
public class SshSessionLifecycleManager {

    private static final Logger log = LoggerFactory.getLogger(SshSessionLifecycleManager.class);

    private final RedisTemplate<String, SessionKey> sessionKeyRedisTemplate;
    private final RedisTemplate<String, SshSessionMetadata> sshSessionMetadataRedisTemplate;
    private final JwtTokenProvider jwtTokenProvider;
    private final SshSessionConfigProperties sshSessionConfigProperties;
    private final JwtConfigProperties jwtConfigProperties;
    private final CustomJschHostKeyRepository customJschHostKeyRepository; // New dependency

    private final ConcurrentMap<SessionKey, SshSessionWrapper> localActiveSshSessions = new ConcurrentHashMap<>();
    private final String applicationInstanceId;

    private static final int JSCH_SESSION_CONNECT_TIMEOUT_MS = 20000; // 20 seconds

    public SshSessionLifecycleManager(
            RedisTemplate<String, SessionKey> sessionKeyRedisTemplate,
            RedisTemplate<String, SshSessionMetadata> sshSessionMetadataRedisTemplate,
            JwtTokenProvider jwtTokenProvider,
            SshSessionConfigProperties sshSessionConfigProperties,
            JwtConfigProperties jwtConfigProperties,
            ApplicationInstanceIdProvider instanceIdProvider,
            CustomJschHostKeyRepository customJschHostKeyRepository) { // Injected provider and new repo
        this.sessionKeyRedisTemplate = sessionKeyRedisTemplate;
        this.sshSessionMetadataRedisTemplate = sshSessionMetadataRedisTemplate;
        this.jwtTokenProvider = jwtTokenProvider;
        this.sshSessionConfigProperties = sshSessionConfigProperties;
        this.jwtConfigProperties = jwtConfigProperties;
        this.applicationInstanceId = instanceIdProvider.getInstanceId() + ":ssh"; // Use provider and append type
        this.customJschHostKeyRepository = customJschHostKeyRepository; // Store new repo
        log.info("SshSessionLifecycleManager initialized with applicationInstanceId: {} and CustomJschHostKeyRepository", this.applicationInstanceId);
    }

    // --- Redis Key Helpers ---
    private String sshTokenRedisKey(String token) {
        return "ssh:token:" + token;
    }

    private String sshSessionMetadataRedisKey(SessionKey key) {
        return "ssh:meta:" + key.userId().toString() + ":" + key.resourceId().toString() + ":" + key.resourceType();
    }

    // --- Public Lifecycle Methods ---

    public SessionResponse initSshSession(UUID platformUserId, UUID serverId, UserProvidedConnectionDetails connectionDetails) {
        SessionKey sessionKey = new SessionKey(platformUserId, serverId, "SSH");
        log.info("Initializing SSH session for key: {} on instance {}", sessionKey, applicationInstanceId);

        // Force release any pre-existing session (local and Redis) for this exact key to ensure clean start.
        // This also means if an init is called for an existing session (e.g. by mistake or to force re-init),
        // the old one is cleaned up before the new one is attempted.
        forceReleaseSessionByKey(sessionKey, false); // false: don't assume a specific existing token needs to be found via metadata for this key

        Supplier<SshSessionWrapper> sshSessionFactory = () -> {
            try {
                JSch jsch = new JSch();
                jsch.setHostKeyRepository(customJschHostKeyRepository); // Set custom HostKeyRepository

                // Add identity: null for name (JSch generates one), no passphrase for key for now
                jsch.addIdentity(
                    UUID.randomUUID().toString(), // Unique name for identity
                    connectionDetails.getDecryptedPrivateKey(),
                    connectionDetails.getPublicKey(), // Optional public key
                    null // Passphrase (byte array), null if no passphrase
                );

                com.jcraft.jsch.Session jschSession = jsch.getSession(
                    connectionDetails.getUsername(),
                    connectionDetails.getHostname(),
                    connectionDetails.getPort()
                );
                // Removed: jschSession.setConfig("StrictHostKeyChecking", "no");
                jschSession.setTimeout(JSCH_SESSION_CONNECT_TIMEOUT_MS);
                log.debug("Attempting JSch session.connect() for key {} with custom host key verification", sessionKey);
                jschSession.connect();
                log.info("JSch session connected successfully for key {} on instance {}", sessionKey, applicationInstanceId);
                return new SshSessionWrapper(sessionKey, jschSession);
            } catch (JSchException e) {
                log.error("JSchException during SSH session factory execution for key {}: {}", sessionKey, e.getMessage(), e);
                throw new RuntimeException("Failed to create SSH session via JSch: " + e.getMessage(), e);
            }
        };

        SshSessionWrapper sessionWrapper;
        try {
            sessionWrapper = sshSessionFactory.get(); // Establish connection
            localActiveSshSessions.put(sessionKey, sessionWrapper); // Store locally
            log.info("Local SSH session created and cached for key {}", sessionKey);
        } catch (Exception e) {
            log.error("Failed to create and store local SSH session for key {}: {}", sessionKey, e.getMessage(), e);
            // No local session created, so no Redis entries to clean up specifically for this attempt.
            throw e; // Re-throw to indicate failure to caller
        }

        String jwtToken = jwtTokenProvider.generateToken(sessionKey);
        long currentTime = System.currentTimeMillis();

        SshSessionMetadata metadata = new SshSessionMetadata(
            sessionKey, currentTime, currentTime, jwtToken, this.applicationInstanceId,
            connectionDetails.getHostname(), connectionDetails.getPort(), connectionDetails.getUsername() // Added extra info
        );

        try {
            sessionKeyRedisTemplate.opsForValue().set(
                sshTokenRedisKey(jwtToken), sessionKey,
                jwtConfigProperties.getExpirationMs(), TimeUnit.MILLISECONDS
            );
            sshSessionMetadataRedisTemplate.opsForValue().set(
                sshSessionMetadataRedisKey(sessionKey), metadata,
                sshSessionConfigProperties.getTimeoutMs(), TimeUnit.MILLISECONDS
            );
            log.info("SSH session metadata and token mapping stored in Redis for key {}", sessionKey);
        } catch (Exception e) {
            log.error("Failed to store session metadata or token in Redis for key {}. Cleaning up local session.", sessionKey, e);
            forceReleaseSessionByKey(sessionKey, false); // Clean up local and any partial Redis state for this key
            throw new RuntimeException("Failed to store session state in Redis: " + e.getMessage(), e);
        }

        return new SessionResponse(jwtToken, jwtConfigProperties.getExpirationMs(), platformUserId, serverId, "SSH");
    }


    public Optional<KeepAliveResponse> keepAliveSshSession(String sessionToken) {
        Optional<SessionKey> keyOpt = jwtTokenProvider.validateTokenAndExtractSessionKey(sessionToken);
        if (keyOpt.isEmpty()) {
            log.warn("Keepalive failed: Invalid or expired JWT token: {}", sessionToken);
            sessionKeyRedisTemplate.delete(sshTokenRedisKey(sessionToken)); // Attempt cleanup of potentially invalid token
            return Optional.empty();
        }
        SessionKey sessionKey = keyOpt.get();

        String metadataRedisKey = sshSessionMetadataRedisKey(sessionKey);
        SshSessionMetadata metadata = sshSessionMetadataRedisTemplate.opsForValue().get(metadataRedisKey);

        if (metadata == null) {
            log.warn("Keepalive failed: No SSH session metadata found in Redis for key {} (from token {}). Cleaning up token mapping.", sessionKey, sessionToken);
            sessionKeyRedisTemplate.delete(sshTokenRedisKey(sessionToken));
            return Optional.empty();
        }

        // Session metadata exists. Update its lastAccessedTime, JWT, and hostingInstanceId (takeover).
        metadata.setLastAccessedTime(System.currentTimeMillis());
        String newJwtToken = jwtTokenProvider.generateToken(sessionKey);
        metadata.setActiveJwtToken(newJwtToken);
        metadata.setHostingInstanceId(this.applicationInstanceId); // This instance now claims to host/manage it

        sshSessionMetadataRedisTemplate.opsForValue().set(
            metadataRedisKey, metadata,
            sshSessionConfigProperties.getTimeoutMs(), TimeUnit.MILLISECONDS
        );

        sessionKeyRedisTemplate.delete(sshTokenRedisKey(sessionToken)); // Delete old token
        sessionKeyRedisTemplate.opsForValue().set(
            sshTokenRedisKey(newJwtToken), sessionKey,
            jwtConfigProperties.getExpirationMs(), TimeUnit.MILLISECONDS
        );
        log.info("SSH Session Keepalive successful for key: {}. Metadata updated, new JWT generated. Hosting instance: {}",
                 sessionKey, this.applicationInstanceId);

        SshSessionWrapper localWrapper = localActiveSshSessions.get(sessionKey);
        if (localWrapper != null) {
            if (localWrapper.isConnected()) {
                localWrapper.updateLastAccessedTime();
                log.debug("Updated lastAccessedTime for local SshSessionWrapper for key {} during keepalive.", sessionKey);
            } else {
                log.warn("Local SSH session wrapper found for key {} during keepalive, but it's disconnected. Releasing from local store.", sessionKey);
                localActiveSshSessions.remove(sessionKey); // Remove dead local session
                localWrapper.disconnect();
                // Metadata in Redis now points here, but we don't have a live session.
                // The next operational request will either fail (if it needs a local session immediately)
                // or attempt re-establishment if that logic is added to operational endpoints.
            }
        } else {
             log.info("No local SSH session for key {} on instance {} during keepalive, though metadata now points here. " +
                      "Session will need to be (re-)established by an operational endpoint if this instance handles it.",
                      sessionKey, this.applicationInstanceId);
        }

        return Optional.of(new KeepAliveResponse(newJwtToken, jwtConfigProperties.getExpirationMs()));
    }

    public void releaseSshSession(String sessionToken) {
        Optional<SessionKey> keyOpt = jwtTokenProvider.validateTokenAndExtractSessionKey(sessionToken);
        // Always try to delete the token from Redis, even if it's invalid/expired.
        // This helps clean up stale token entries if the JWT validation itself fails.
        sessionKeyRedisTemplate.delete(sshTokenRedisKey(sessionToken));

        if (keyOpt.isEmpty()) {
            log.warn("Release attempt with invalid or expired JWT token: {}. Token mapping deleted (if existed).", sessionToken);
            return;
        }
        SessionKey sessionKey = keyOpt.get();
        log.info("Attempting to release SSH session for key: {} (derived from token {})", sessionKey, sessionToken);
        forceReleaseSessionByKey(sessionKey, false); // false: token already deleted by this method
    }

    /**
     * Helper to get a SshSessionWrapper if it's local.
     * This does NOT check Redis metadata.
     * Updates last accessed time on the local wrapper if found and connected.
     * Important: Does NOT update Redis metadata's lastAccessedTime. That's for operational methods or keepalive.
     */
    public Optional<SshSessionWrapper> getLocalSession(SessionKey key) {
        SshSessionWrapper wrapper = localActiveSshSessions.get(key);
        if (wrapper != null) {
            if (wrapper.isConnected()) {
                wrapper.updateLastAccessedTime(); // Update local access time
                return Optional.of(wrapper);
            } else {
                log.warn("Local session for key {} is disconnected. Removing from local cache.", key);
                localActiveSshSessions.remove(key); // Remove if disconnected
                // Consider full forceReleaseSessionByKey(key) if it should also clean Redis metadata
            }
        }
        return Optional.empty();
    }

    /**
     * Retrieves session metadata from Redis. Does not affect local sessions.
     * Made public for SshOperationController.
     */
    public Optional<SshSessionMetadata> getSessionMetadata(SessionKey key) {
        if (key == null) return Optional.empty();
        return Optional.ofNullable(sshSessionMetadataRedisTemplate.opsForValue().get(sshSessionMetadataRedisKey(key)));
    }

    /**
     * Updates the lastAccessedTime in Redis metadata for a given session key
     * AND for the local SshSessionWrapper if present and connected on this instance.
     * This method is intended to be called when an operation happens on a session.
     * It also updates the hostingInstanceId in metadata to the current instance,
     * effectively allowing an active operation to signal this instance now manages this session.
     */
    public void updateSessionAccessTime(SessionKey key) { // Renamed from plan's updateSessionAccessTime(key, wrapper)
        if (key == null) return;

        SshSessionWrapper localWrapper = localActiveSshSessions.get(key);
        long newLastAccessedTime = System.currentTimeMillis();

        if (localWrapper != null && localWrapper.isConnected()) {
            localWrapper.updateLastAccessedTime(); // This sets it to System.currentTimeMillis()
            newLastAccessedTime = localWrapper.getLastAccessedTime(); // Use the wrapper's time
            log.debug("Updated local SshSessionWrapper lastAccessTime for key {}", key);
        }

        SshSessionMetadata metadata = sshSessionMetadataRedisTemplate.opsForValue().get(sshSessionMetadataRedisKey(key));
        if (metadata != null) {
            metadata.setLastAccessedTime(newLastAccessedTime);
            // If an operation occurs, this instance is effectively the host.
            // This helps in scenarios where a keepalive might have shifted expected host,
            // but an operation request lands here and succeeds.
            metadata.setHostingInstanceId(this.applicationInstanceId);
            sshSessionMetadataRedisTemplate.opsForValue().set(
                sshSessionMetadataRedisKey(key), metadata,
                sshSessionConfigProperties.getTimeoutMs(), TimeUnit.MILLISECONDS);
            log.debug("Updated lastAccessedTime and hostingInstanceId in Redis metadata for key {}", key);
        } else {
            log.warn("Attempted to update access time for key {}, but no metadata found in Redis.", key);
            // If we have a local session but no metadata, this is an inconsistency.
            // We might re-create metadata here if a local session exists.
            if (localWrapper != null && localWrapper.isConnected()) {
                 log.warn("Re-creating missing metadata for active local session on key {} during access time update.", key);
                 SshSessionMetadata newMetaData = new SshSessionMetadata(
                    key, localWrapper.getCreatedAt(), newLastAccessedTime,
                    null, // We don't know the JWT here easily, it's mapped separately.
                    this.applicationInstanceId,
                    localWrapper.getJschSession().getHost(), // Example extra info
                    localWrapper.getJschSession().getPort(),
                    localWrapper.getJschSession().getUserName()
                 );
                sshSessionMetadataRedisTemplate.opsForValue().set(
                    sshSessionMetadataRedisKey(key), newMetaData,
                    sshSessionConfigProperties.getTimeoutMs(), TimeUnit.MILLISECONDS
                );
            }
        }
    }


    private void forceReleaseSessionByKey(SessionKey key, boolean tokenKnownInMetadata) {
        if (key == null) return;

        log.info("Force releasing session for key: {} on instance {}", key, applicationInstanceId);
        SshSessionWrapper localWrapper = localActiveSshSessions.remove(key);
        if (localWrapper != null) {
            log.debug("Disconnecting local SSH session for key {}", key);
            localWrapper.disconnect();
        }

        SshSessionMetadata metadata = sshSessionMetadataRedisTemplate.opsForValue().get(sshSessionMetadataRedisKey(key));
        sshSessionMetadataRedisTemplate.delete(sshSessionMetadataRedisKey(key));
        log.debug("Deleted SSH session metadata from Redis for key {}", key);

        if (tokenKnownInMetadata && metadata != null && metadata.getActiveJwtToken() != null) {
            sessionKeyRedisTemplate.delete(sshTokenRedisKey(metadata.getActiveJwtToken()));
            log.debug("Deleted JWT-to-SessionKey mapping from Redis for token associated with key {} (from metadata)", key);
        } else if (!tokenKnownInMetadata && metadata != null && metadata.getActiveJwtToken() != null) {
            // If token was not known (e.g. release by key directly, or init cleanup) but metadata had one, delete it.
            log.warn("forceReleaseSessionByKey (tokenNotKnown) is deleting token {} from metadata for key {}", metadata.getActiveJwtToken(), key);
            sessionKeyRedisTemplate.delete(sshTokenRedisKey(metadata.getActiveJwtToken()));
        }
        // If tokenKnownInMetadata is false, it means the caller (e.g. releaseSshSession(token)) already handled token deletion.
    }

    @Scheduled(fixedDelayString = "${codebridge.session.ssh.timeout-ms:600000}", initialDelay = 120000) // e.g., every 10 mins, initial delay 2 mins
    public void cleanupExpiredSessions() {
        log.info("Starting expired local SSH session cleanup task on instance {}...", applicationInstanceId);
        long timeoutMs = sshSessionConfigProperties.getTimeoutMs();
        long currentTime = System.currentTimeMillis();
        int expiredLocalCount = 0;

        Iterator<Map.Entry<SessionKey, SshSessionWrapper>> iterator = localActiveSshSessions.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<SessionKey, SshSessionWrapper> entry = iterator.next();
            SessionKey key = entry.getKey();
            SshSessionWrapper wrapper = entry.getValue();

            if (!wrapper.isConnected() || (currentTime - wrapper.getLastAccessedTime()) > timeoutMs) {
                log.info("Local SSH session for key: {} has expired or disconnected (last accessed: {}ms ago, connected: {}). Cleaning up.",
                         key, (currentTime - wrapper.getLastAccessedTime()), wrapper.isConnected());
                iterator.remove(); // Remove from local map first
                // Then call forceRelease which handles disconnect and Redis cleanup
                forceReleaseSessionByKey(key, true); // true: try to find token from metadata to clean token map
                expiredLocalCount++;
            }
        }
        if (expiredLocalCount > 0) {
            log.info("Local SSH session cleanup task completed on instance {}. Removed {} sessions.",
                     applicationInstanceId, expiredLocalCount);
        } else {
            log.debug("Local SSH session cleanup task completed on instance {}. No local sessions expired or disconnected.", applicationInstanceId);
        }
    }
}
