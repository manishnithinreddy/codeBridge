package com.codebridge.session.service;

import com.codebridge.session.model.KnownSshHostKey;
import com.codebridge.session.repository.KnownSshHostKeyRepository;
import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.HostKeyRepository;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.UserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
public class CustomJschHostKeyRepository implements HostKeyRepository {

    private static final Logger logger = LoggerFactory.getLogger(CustomJschHostKeyRepository.class);
    private final KnownSshHostKeyRepository knownSshHostKeyRepository;
    
    // Host key verification policy
    public enum HostKeyVerificationPolicy {
        STRICT,      // Reject unknown or changed keys
        ASK,         // Ask for confirmation (if UI available)
        AUTO_ACCEPT  // Auto-accept unknown keys (Trust On First Use)
    }
    
    private HostKeyVerificationPolicy verificationPolicy = HostKeyVerificationPolicy.AUTO_ACCEPT;

    public CustomJschHostKeyRepository(KnownSshHostKeyRepository knownSshHostKeyRepository) {
        this.knownSshHostKeyRepository = knownSshHostKeyRepository;
    }
    
    /**
     * Set the host key verification policy
     * @param policy The policy to use
     */
    public void setVerificationPolicy(HostKeyVerificationPolicy policy) {
        this.verificationPolicy = policy;
        logger.info("Host key verification policy set to: {}", policy);
    }
    
    /**
     * Get the current host key verification policy
     * @return The current policy
     */
    public HostKeyVerificationPolicy getVerificationPolicy() {
        return this.verificationPolicy;
    }

    @Override
    @Transactional(readOnly = true) // Read operation
    public int check(String host, byte[] key) {
        String keyType = getKeyTypeFromBytes(key); // Helper to determine key type, e.g. "ssh-rsa"
        if (keyType == null) {
            logger.warn("Could not determine key type for host {} during check.", host);
            return NOT_INCLUDED; // Or treat as error
        }

        // JSch might pass host as "[hostname]:port"
        String hostname = host;
        int port = 22; // Default SSH port
        if (host.startsWith("[") && host.contains("]:")) {
            hostname = host.substring(1, host.indexOf("]:"));
            try {
                port = Integer.parseInt(host.substring(host.indexOf("]:") + 2));
            } catch (NumberFormatException e) {
                logger.warn("Could not parse port from host string: {}", host);
            }
        } else if (host.contains(":")) {
             try {
                port = Integer.parseInt(host.substring(host.indexOf(":") + 1));
                hostname = host.substring(0, host.indexOf(":"));
            } catch (NumberFormatException e) {
                 logger.warn("Could not parse port from host string: {}", host);
                 // hostname remains as is
            }
        }

        Optional<KnownSshHostKey> existingKeyOpt =
            knownSshHostKeyRepository.findByHostnameAndPortAndKeyType(hostname, port, keyType);

        if (existingKeyOpt.isPresent()) {
            KnownSshHostKey existingKey = existingKeyOpt.get();
            byte[] storedKeyBytes = Base64.getDecoder().decode(existingKey.getHostKeyBase64());
            if (Arrays.equals(storedKeyBytes, key)) {
                logger.debug("Host key for [{}:{}], type {} VERIFIED.", hostname, port, keyType);
                // Optionally update lastVerified timestamp here if desired, but check is read-only
                return OK;
            } else {
                logger.warn("!!! HOST KEY MISMATCH for [{}:{}], type {} !!!", hostname, port, keyType);
                logger.warn("Presented fingerprint: {}", generateFingerprint(key));
                logger.warn("Stored fingerprint: {}", existingKey.getFingerprintSha256());
                
                // If policy is STRICT, always return CHANGED (will cause connection to fail)
                if (verificationPolicy == HostKeyVerificationPolicy.STRICT) {
                    return CHANGED;
                }
                
                // For ASK and AUTO_ACCEPT, we'll handle in the add() method when JSch calls it
                return CHANGED;
            }
        }
        
        logger.info("Host key for [{}:{}], type {} NOT_INCLUDED in known hosts.", hostname, port, keyType);
        
        // If policy is STRICT, reject unknown keys
        if (verificationPolicy == HostKeyVerificationPolicy.STRICT) {
            logger.warn("Rejecting unknown host key due to STRICT policy");
            return NOT_INCLUDED;
        }
        
        // For ASK and AUTO_ACCEPT, we'll handle in the add() method when JSch calls it
        return NOT_INCLUDED;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW) // Ensure this runs in its own transaction
    public void add(HostKey hostkey, UserInfo ui) {
        String hostname = hostkey.getHost();
        int port = 22; // JSch HostKey class might not always parse port from host string
        // Try to parse port from hostname if JSch doesn't do it consistently for HostKey.getHost()
        if (hostname.startsWith("[") && hostname.contains("]:")) {
            try {
                port = Integer.parseInt(hostname.substring(hostname.indexOf("]:") + 2));
                hostname = hostname.substring(1, hostname.indexOf("]:"));
            } catch (NumberFormatException e) { /* use default port */ }
        } else if (hostname.contains(":")) {
            try {
                port = Integer.parseInt(hostname.substring(hostname.indexOf(":") + 1));
                hostname = hostname.substring(0, hostname.indexOf(":"));
            } catch (NumberFormatException e) { /* use default port */ }
        }

        String keyType = hostkey.getType();
        String keyBase64 = hostkey.getKey(); // This is already Base64 encoded by JSch
        String fingerprint = generateFingerprint(Base64.getDecoder().decode(keyBase64));

        // Check if this is a key change (rather than a new key)
        boolean isKeyChange = false;
        Optional<KnownSshHostKey> existingKeyOpt = 
            knownSshHostKeyRepository.findByHostnameAndPortAndKeyType(hostname, port, keyType);
        if (existingKeyOpt.isPresent()) {
            isKeyChange = true;
        }

        // Determine if we should trust this key based on policy and UI
        boolean trusted = false;
        
        if (verificationPolicy == HostKeyVerificationPolicy.AUTO_ACCEPT) {
            // Auto-accept for TOFU (Trust On First Use) or if policy is set to auto-accept
            trusted = true;
            if (isKeyChange) {
                logger.warn("Auto-accepting CHANGED host key for [{}:{}] type {} due to AUTO_ACCEPT policy", 
                    hostname, port, keyType);
            } else {
                logger.info("Auto-accepting new host key for [{}:{}] type {} due to AUTO_ACCEPT policy", 
                    hostname, port, keyType);
            }
        } else if (verificationPolicy == HostKeyVerificationPolicy.ASK && ui != null) {
            // If UI is available and policy is ASK, prompt the user
            String message;
            if (isKeyChange) {
                message = String.format(
                    "WARNING: REMOTE HOST IDENTIFICATION HAS CHANGED!%n" +
                    "IT IS POSSIBLE THAT SOMEONE IS DOING SOMETHING NASTY!%n" +
                    "The %s host key for [%s]:%d has changed.%n" +
                    "New fingerprint: %s%n" +
                    "Are you sure you want to continue connecting (yes/no)?",
                    keyType, hostname, port, fingerprint);
            } else {
                message = String.format(
                    "The authenticity of host '[%s]:%d' can't be established.%n" +
                    "%s key fingerprint is %s.%n" +
                    "Are you sure you want to continue connecting (yes/no)?",
                    hostname, port, keyType, fingerprint);
            }
            
            trusted = ui.promptYesNo(message);
            if (trusted) {
                logger.info("User accepted {} host key for [{}:{}]", 
                    isKeyChange ? "changed" : "new", hostname, port);
            } else {
                logger.warn("User rejected {} host key for [{}:{}]", 
                    isKeyChange ? "changed" : "new", hostname, port);
            }
        } else {
            // STRICT policy or ASK with no UI - reject
            logger.warn("Rejecting {} host key for [{}:{}] due to {} policy with {} UI", 
                isKeyChange ? "changed" : "new", hostname, port, 
                verificationPolicy, ui == null ? "no" : "available");
            trusted = false;
        }

        if (trusted) {
            // If it's a key change, update the existing record
            KnownSshHostKey keyRecord = existingKeyOpt.orElse(new KnownSshHostKey());
            keyRecord.setHostname(hostname);
            keyRecord.setPort(port);
            keyRecord.setKeyType(keyType);
            keyRecord.setHostKeyBase64(keyBase64);
            keyRecord.setFingerprintSha256(fingerprint);
            keyRecord.setLastVerified(LocalDateTime.now());
            
            // Only set firstSeen for new keys
            if (!existingKeyOpt.isPresent()) {
                keyRecord.setFirstSeen(LocalDateTime.now());
            }

            knownSshHostKeyRepository.save(keyRecord);
            logger.info("{} host key in database for [{}:{}] type {}. Fingerprint: {}", 
                existingKeyOpt.isPresent() ? "Updated" : "Added new", 
                hostname, port, keyType, fingerprint);
        } else {
            logger.warn("Host key for [{}:{}] type {} was NOT added/updated.", hostname, port, keyType);
            // Throw exception to abort the connection if key was rejected
            throw new RuntimeException("Host key verification failed: Key was rejected");
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void remove(String host, String type, byte[] key) {
        // JSch might pass host as "[hostname]:port"
        String hostname = host;
        int port = 22;
         if (host.startsWith("[") && host.contains("]:")) {
            hostname = host.substring(1, host.indexOf("]:"));
            try {
                port = Integer.parseInt(host.substring(host.indexOf("]:") + 2));
            } catch (NumberFormatException e) { /* use default */ }
        } else if (host.contains(":")) {
             try {
                port = Integer.parseInt(host.substring(host.indexOf(":") + 1));
                hostname = host.substring(0, host.indexOf(":"));
            } catch (NumberFormatException e) { /* use default */ }
        }

        String keyTypeToRemove = type;
        if (key != null && type == null) { // If type is null, try to derive from key
            keyTypeToRemove = getKeyTypeFromBytes(key);
        }

        if (keyTypeToRemove != null) {
            Optional<KnownSshHostKey> existingKeyOpt =
                knownSshHostKeyRepository.findByHostnameAndPortAndKeyType(hostname, port, keyTypeToRemove);
            existingKeyOpt.ifPresent(knownSshHostKeyRepository::delete);
            logger.info("Removed host key from database for [{}:{}] type {}", hostname, port, keyTypeToRemove);
        } else if (key == null && type == null) { // Remove all keys for host
             List<KnownSshHostKey> keysForHost = knownSshHostKeyRepository.findByHostnameAndPort(hostname, port);
             knownSshHostKeyRepository.deleteAll(keysForHost);
             logger.info("Removed all host keys from database for [{}:{}]", hostname, port);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public HostKey[] getHostKey() { // Get all known keys
        List<KnownSshHostKey> allDbKeys = knownSshHostKeyRepository.findAll();
        return allDbKeys.stream().map(this::mapDbKeyToJschHostKey).filter(Objects::nonNull).toArray(HostKey[]::new);
    }

    @Override
    @Transactional(readOnly = true)
    public HostKey[] getHostKey(String host, String type) {
         // JSch might pass host as "[hostname]:port"
        String hostname = host;
        int port = 22;
         if (host.startsWith("[") && host.contains("]:")) {
            hostname = host.substring(1, host.indexOf("]:"));
            try {
                port = Integer.parseInt(host.substring(host.indexOf("]:") + 2));
            } catch (NumberFormatException e) { /* use default */ }
        } else if (host.contains(":")) {
             try {
                port = Integer.parseInt(host.substring(host.indexOf(":") + 1));
                hostname = host.substring(0, host.indexOf(":"));
            } catch (NumberFormatException e) { /* use default */ }
        }

        List<KnownSshHostKey> dbKeys;
        if (type != null) {
            dbKeys = knownSshHostKeyRepository.findByHostnameAndPortAndKeyType(hostname, port, type).map(List::of).orElse(List.of());
        } else {
            dbKeys = knownSshHostKeyRepository.findByHostnameAndPort(hostname, port);
        }
        return dbKeys.stream().map(this::mapDbKeyToJschHostKey).filter(Objects::nonNull).toArray(HostKey[]::new);
    }

    @Override
    public String getKnownHostsRepositoryID() {
        return "CodebridgeSessionServiceDBHostKeyRepository";
    }

    // --- Helper Methods ---
    private HostKey mapDbKeyToJschHostKey(KnownSshHostKey dbKey) {
        try {
            // Construct host string for JSch, potentially including port if not default
            String hostString = dbKey.getPort() != 22 ? String.format("[%s]:%d", dbKey.getHostname(), dbKey.getPort()) : dbKey.getHostname();
            return new HostKey(hostString, HostKey.GUESS, Base64.getDecoder().decode(dbKey.getHostKeyBase64()));
            // Or: new HostKey(dbKey.getHostname(), dbKey.getPort(), HostKey.GUESS, Base64.getDecoder().decode(dbKey.getHostKeyBase64()));
            // HostKey.GUESS will make JSch determine type from key bytes. Or use dbKey.getKeyType() if it matches JSch types.
        } catch (JSchException e) {
            logger.error("Error converting DB host key to JSch HostKey for host {}: {}", dbKey.getHostname(), e.getMessage());
            return null;
        }
    }

    private String getKeyTypeFromBytes(byte[] keyBytes) {
        if (keyBytes == null || keyBytes.length < 4) {
            return null;
        }
        
        try {
            // Try to create a temporary HostKey to get its type
            HostKey tempHostKey = new HostKey("temp", HostKey.GUESS, keyBytes);
            return tempHostKey.getType();
        } catch (JSchException e) {
            logger.debug("Could not determine key type using JSch: {}", e.getMessage());
            
            // Fallback: Try to parse the key type from the binary format
            // SSH key format: [length][type][key data]
            try {
                // First 4 bytes are the length of the type string
                int typeLength = ((keyBytes[0] & 0xFF) << 24) | 
                                 ((keyBytes[1] & 0xFF) << 16) | 
                                 ((keyBytes[2] & 0xFF) << 8) | 
                                 (keyBytes[3] & 0xFF);
                
                if (typeLength > 0 && typeLength < 20 && typeLength + 4 <= keyBytes.length) {
                    return new String(keyBytes, 4, typeLength, StandardCharsets.UTF_8);
                }
            } catch (Exception ex) {
                logger.debug("Failed to parse key type from binary format: {}", ex.getMessage());
            }
            
            // Last resort: Check for common patterns in the key
            if (bytesContain(keyBytes, "ssh-rsa")) return "ssh-rsa";
            if (bytesContain(keyBytes, "ssh-dss")) return "ssh-dss";
            if (bytesContain(keyBytes, "ecdsa-sha2-nistp256")) return "ecdsa-sha2-nistp256";
            if (bytesContain(keyBytes, "ecdsa-sha2-nistp384")) return "ecdsa-sha2-nistp384";
            if (bytesContain(keyBytes, "ecdsa-sha2-nistp521")) return "ecdsa-sha2-nistp521";
            if (bytesContain(keyBytes, "ssh-ed25519")) return "ssh-ed25519";
            
            return "unknown";
        }
    }
    private boolean bytesContain(byte[] source, String searchText) {
        return new String(source, 0, Math.min(source.length, 50), StandardCharsets.US_ASCII).contains(searchText);
    }


    private String generateFingerprint(byte[] publicKeyBytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(publicKeyBytes);
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            logger.error("Error generating SHA-256 fingerprint for public key", e);
            return "error-generating-fingerprint";
        }
    }
}
