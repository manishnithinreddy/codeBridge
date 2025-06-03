package com.codebridge.session.service;

import com.codebridge.session.model.KnownSshHostKey;
import com.codebridge.session.repository.KnownSshHostKeyRepository;
import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.HostKeyRepository;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.UserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.security.MessageDigest; // For fingerprint generation

@Component
public class CustomJschHostKeyRepository implements HostKeyRepository {

    private static final Logger logger = LoggerFactory.getLogger(CustomJschHostKeyRepository.class);
    private final KnownSshHostKeyRepository knownSshHostKeyRepository;

    public CustomJschHostKeyRepository(KnownSshHostKeyRepository knownSshHostKeyRepository) {
        this.knownSshHostKeyRepository = knownSshHostKeyRepository;
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
                return CHANGED;
            }
        }
        logger.info("Host key for [{}:{}], type {} NOT_INCLUDED in known hosts.", hostname, port, keyType);
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

        // TOFU: Trust On First Use.
        // If ui is null or doesn't prompt, we might auto-accept.
        // For a backend service, auto-accepting is common for the first connection.
        boolean trusted = true; 
        if (ui != null) {
            // Example prompt, though for backend, this might be pre-approved or logged
            String message = String.format("The authenticity of host '[%s]:%d' can't be established.%n" +
                                           "%s key fingerprint is %s.%n" +
                                           "Are you sure you want to continue connecting (yes/no)?",
                                           hostname, port, keyType, fingerprint);
            // trusted = ui.promptYesNo(message); // This would block if UI is interactive
            logger.info("TOFU: {}", message);
            logger.info("Auto-accepting new host key for [{}:{}] type {} (fingerprint: {}) due to backend TOFU policy.", hostname, port, keyType, fingerprint);
        } else {
             logger.info("Auto-accepting new host key for [{}:{}] type {} (fingerprint: {}) due to backend TOFU policy (no UserInfo).", hostname, port, keyType, fingerprint);
        }

        if (trusted) {
            KnownSshHostKey newKey = new KnownSshHostKey();
            newKey.setHostname(hostname);
            newKey.setPort(port);
            newKey.setKeyType(keyType);
            newKey.setHostKeyBase64(keyBase64);
            newKey.setFingerprintSha256(fingerprint);
            // firstSeen and lastVerified will be set by @CreationTimestamp and @UpdateTimestamp
            newKey.setFirstSeen(LocalDateTime.now()); // Explicitly set for clarity
            newKey.setLastVerified(LocalDateTime.now());

            knownSshHostKeyRepository.save(newKey);
            logger.info("Added new host key to database for [{}:{}] type {}. Fingerprint: {}", hostname, port, keyType, fingerprint);
        } else {
            logger.warn("Host key for [{}:{}] type {} was NOT added by user choice.", hostname, port, keyType);
            // Optionally throw an exception here if strictness is required
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
        // This is a simplified way; JSch does this internally more robustly.
        // For common types:
        if (keyBytes == null) return null;
        String keyString = new String(keyBytes, StandardCharsets.UTF_8); // This is not right for raw key bytes.
        // A proper way would be to use JSch's internal parsing or a library.
        // For example, if keyBytes are the full public key line (e.g. "ssh-rsa AAAA...")
        // This placeholder needs a robust implementation. For now, let's assume key type is provided or guessed by JSch.
        // JSch's HostKey class can often guess the type if given the raw key material.
        // We might have to create a temporary HostKey object to get its type.
        try {
            // This is a bit circular, but can work if JSch can parse it
            HostKey tempHostKey = new HostKey("dummyHost", HostKey.GUESS, keyBytes);
            return tempHostKey.getType();
        } catch (JSchException e) {
            logger.warn("Could not guess key type from bytes: {}", e.getMessage());
            // Fallback for some common patterns if the above fails (very basic)
            // This is NOT a reliable way to get key type from raw public key bytes.
            // JSch's `HostKey.getType()` from `HostKey(host, type, key)` with `type=GUESS` is better.
            // The `key` byte array in `check(host, key)` is the public key blob, not the full text line.
            if (keyBytes.length > 10 && keyBytes[8] == 's' && keyBytes[9] == 's' && keyBytes[10] == 'h') { // Heuristic
                 if (bytesContain(keyBytes, "ssh-rsa")) return "ssh-rsa";
                 if (bytesContain(keyBytes, "ssh-dss")) return "ssh-dss";
                 if (bytesContain(keyBytes, "ecdsa-sha2-nistp256")) return "ecdsa-sha2-nistp256";
                 if (bytesContain(keyBytes, "ssh-ed25519")) return "ssh-ed25519";
            }
        }
        return "unknown"; // Fallback
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
