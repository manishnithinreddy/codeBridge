package com.codebridge.session.service;

import com.codebridge.session.model.KnownSshHostKey;
import com.codebridge.session.repository.KnownSshHostKeyRepository;
import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.HostKeyRepository;
import com.jcraft.jsch.UserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Component
public class CustomJschHostKeyRepository implements HostKeyRepository {

    private static final Logger log = LoggerFactory.getLogger(CustomJschHostKeyRepository.class);

    private final KnownSshHostKeyRepository knownSshHostKeyRepository;

    public CustomJschHostKeyRepository(KnownSshHostKeyRepository knownSshHostKeyRepository) {
        this.knownSshHostKeyRepository = knownSshHostKeyRepository;
    }

    private String calculateSha256Fingerprint(byte[] key) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(key);
            return Base64.getEncoder().encodeToString(digest); // Standard SHA256 fingerprint format often uses Base64
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not found for fingerprint calculation", e);
            return null; // Or throw an exception
        }
    }

    private ParsedHost parseHost(String jschHostString) {
        // JSch host string can be "[host]:port" or just "host"
        // This simple parser assumes port is only specified with brackets.
        // JSch's HostKey object itself should have parsed host/port if available.
        String hostname = jschHostString;
        int port = -1; // JSch default is 22, but HostKey might provide it

        if (jschHostString.startsWith("[")) {
            int closingBracketIndex = jschHostString.indexOf(']');
            if (closingBracketIndex > 0) {
                hostname = jschHostString.substring(1, closingBracketIndex);
                if (jschHostString.length() > closingBracketIndex + 1 && jschHostString.charAt(closingBracketIndex + 1) == ':') {
                    try {
                        port = Integer.parseInt(jschHostString.substring(closingBracketIndex + 2));
                    } catch (NumberFormatException e) {
                        log.warn("Could not parse port from JSch host string: {}", jschHostString);
                    }
                }
            }
        }
        return new ParsedHost(hostname, port);
    }


    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW) // Ensure DB operation completes even if outer Tx rolls back
    public int check(String host, byte[] key) {
        // Host from JSch might be just hostname or [hostname]:port
        // We need to rely on the HostKey object in add() for definitive port, or parse carefully.
        // For check, JSch usually provides the resolved hostname.
        // The key provided here is the public key received from the server.

        // For simplicity, let's assume 'host' is just the hostname and we use a default/known port if not specified.
        // However, JSch's HostKey object (passed in add method) is more reliable for host/port.
        // This 'host' parameter in check() might not include port.
        // We need to get the actual port the JSch session is connecting to.
        // This method is problematic if port isn't passed or derivable reliably.
        // Let's assume for this example, we'd need a way to get the current Session's port if host doesn't contain it.
        // For now, we proceed cautiously. JSch's typical flow involves calling getHostKey() first if available.

        log.debug("HostKeyRepository.check called for host: {}", host);
        // We cannot get port reliably from `host` string alone here easily if it's not `[host]:port`.
        // JSch's own HostKey object is more definitive. This check method is tricky.
        // Let's assume the `host` is the pure hostname for lookup for now, and rely on `add` for port.
        // This means we might not find specific port-differentiated keys here if only hostname is passed.

        // A better `check` would need the port. We might need to look up all keys for the host
        // and then try to match the key type and key itself.

        String connectingKeyType = ""; // We need to determine this from 'key'
        com.jcraft.jsch.HostKey tempHostKeyForTypeExtraction;
        try {
            // Hostname here is a placeholder for HostKey constructor. Type is what we need.
             tempHostKeyForTypeExtraction = new com.jcraft.jsch.HostKey("placeholder", key);
             connectingKeyType = tempHostKeyForTypeExtraction.getType();
        } catch (JSchException e) {
            log.error("Could not determine key type from received public key for host {}: {}", host, e.getMessage());
            return HostKeyRepository.NOT_INCLUDED; // Cannot proceed without key type
        }

        // Find by hostname only, then filter by key type and key. Port is an issue here.
        // This is a limitation of JSch's HostKeyRepository interface's check method signature.
        // We'll assume for now that `host` is unique enough or we iterate through all matching hostnames.
        List<KnownSshHostKey> entries = knownSshHostKeyRepository.findAll(); // Inefficient: find by hostname if possible
        KnownSshHostKey matchedEntry = null;
        boolean keyTypeFound = false;

        for (KnownSshHostKey knownKey : entries) {
            if (!knownKey.getHostname().equalsIgnoreCase(host)) continue; // Simple hostname match for now

            if (knownKey.getKeyType().equals(connectingKeyType)) {
                keyTypeFound = true;
                byte[] storedKeyBytes = Base64.getDecoder().decode(knownKey.getHostKeyBase64());
                if (java.util.Arrays.equals(storedKeyBytes, key)) {
                    matchedEntry = knownKey;
                    break;
                }
            }
        }

        if (matchedEntry != null) {
            log.info("Host key for host {} (type {}) found and matches. Updating lastVerified.", host, connectingKeyType);
            matchedEntry.setLastVerified(LocalDateTime.now());
            knownSshHostKeyRepository.save(matchedEntry);
            return HostKeyRepository.OK;
        } else if (keyTypeFound) {
            // A key of the same type was found for this host, but it's different.
            log.warn("!!! HOST KEY CHANGED for host {} (type {}) !!! Potential MITM attack!", host, connectingKeyType);
            return HostKeyRepository.CHANGED;
        } else {
            // No key of this type found for this host.
            log.info("No host key of type {} found for host {}. Will attempt to add (TOFU).", connectingKeyType, host);
            return HostKeyRepository.NOT_INCLUDED; // JSch will call add()
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void add(HostKey hostkey, UserInfo ui) {
        log.info("HostKeyRepository.add called for host: {}, type: {}", hostkey.getHost(), hostkey.getType());

        // UserInfo might be null in non-interactive scenarios.
        // TOFU: Trust on First Use - automatically add the key.
        // If UI is available, could prompt, but for a backend service, auto-add is common for TOFU.
        if (ui != null && ui.promptYesNo("The authenticity of host '" + hostkey.getHost() + "' can't be established.\n"
                                       + hostkey.getType() + " key fingerprint is " + hostkey.getFingerPrint(new JSch()) + ".\n"
                                       + "Are you sure you want to continue connecting?")) {
            // User said yes (or no prompt if ui is null/non-interactive)
        } else if (ui != null) {
            log.warn("User declined to add new host key for {}.", hostkey.getHost());
            // JSch will typically throw an exception if host key is not accepted.
            return;
        }
        // If ui is null, proceed with TOFU:

        String hostname = hostkey.getHost();
        int port = hostkey.getPort();
        if (port <=0) port = 22; // JSch default, HostKey.getPort() might be -1

        // Check if we already have this exact key, possibly due to race or slightly different host string
         List<KnownSshHostKey> existingKeys = knownSshHostKeyRepository.findByHostnameAndPort(hostname, port);
         for (KnownSshHostKey existing : existingKeys) {
             if (existing.getKeyType().equals(hostkey.getType())) {
                 byte[] storedKeyBytes = Base64.getDecoder().decode(existing.getHostKeyBase64());
                 if (java.util.Arrays.equals(storedKeyBytes, hostkey.getKey())) {
                     log.info("Host key for {} port {} type {} already exists. Updating lastVerified.", hostname, port, hostkey.getType());
                     existing.setLastVerified(LocalDateTime.now());
                     knownSshHostKeyRepository.save(existing);
                     return;
                 }
             }
         }


        KnownSshHostKey newKey = new KnownSshHostKey();
        newKey.setHostname(hostname);
        newKey.setPort(port);
        newKey.setKeyType(hostkey.getType());
        newKey.setHostKeyBase64(Base64.getEncoder().encodeToString(hostkey.getKey()));
        newKey.setFingerprintSha256(calculateSha256Fingerprint(hostkey.getKey())); // Calculate and store fingerprint
        newKey.setFirstSeen(LocalDateTime.now());
        newKey.setLastVerified(LocalDateTime.now());

        knownSshHostKeyRepository.save(newKey);
        log.info("Added new SSH host key to database: Host={}, Port={}, Type={}, FingerprintSHA256={}",
                 hostname, port, newKey.getKeyType(), newKey.getFingerprintSha256());
    }

    @Override
    public void remove(String host, String type) {
        // This host parameter might be "hostname" or "[hostname]:port" or just hostname from known_hosts file.
        // Type can be null to remove all keys for the host.
        log.debug("HostKeyRepository.remove called for host: {}, type: {}", host, type);
        // For simplicity, we'll assume host is just hostname. If port differentiation is needed, parsing is required.
        List<KnownSshHostKey> keysToRemove = knownSshHostKeyRepository.findByHostnameAndPort(host, -1); // -1 to ignore port or handle all
        if (type != null) {
            keysToRemove.removeIf(k -> !k.getKeyType().equals(type));
        }
        if (!keysToRemove.isEmpty()) {
            knownSshHostKeyRepository.deleteAll(keysToRemove);
            log.info("Removed {} host keys for host '{}' (type: {})", keysToRemove.size(), host, type);
        }
    }

    @Override
    public void remove(String host, String type, byte[] key) {
        // More specific remove: by host, type, and specific key.
        log.debug("HostKeyRepository.remove called for host: {}, type: {}, specific key", host, type);
        // This parsing logic is simplified, real HostKey objects are better.
        ParsedHost pHost = parseHost(host);
        int port = pHost.port > 0 ? pHost.port : 22; // Default if not specified

        Optional<KnownSshHostKey> keyOpt = knownSshHostKeyRepository.findByHostnameAndPortAndKeyType(pHost.hostname, port, type);
        keyOpt.ifPresent(knownKey -> {
            byte[] storedKeyBytes = Base64.getDecoder().decode(knownKey.getHostKeyBase64());
            if (java.util.Arrays.equals(storedKeyBytes, key)) {
                knownSshHostKeyRepository.delete(knownKey);
                log.info("Removed specific host key for host '{}', port {}, type '{}'", pHost.hostname, port, type);
            }
        });
    }


    @Override
    public String getKnownHostsRepositoryID() {
        return "CodeBridgeDBHostKeyRepository";
    }

    @Override
    public HostKey[] getHostKey() {
        // Return all keys from DB. JSch might use this to populate its initial list.
        // This could be large. JSch also calls getHostKey(host, type).
        List<KnownSshHostKey> allDbKeys = knownSshHostKeyRepository.findAll();
        return allDbKeys.stream()
                .map(this::convertToJschHostKey)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toArray(HostKey[]::new);
    }

    @Override
    public HostKey[] getHostKey(String host, String type) {
        // This host parameter might be "hostname" or "[hostname]:port".
        // Type can be null.
        log.debug("HostKeyRepository.getHostKey called for host: {}, type: {}", host, type);
        ParsedHost pHost = parseHost(host);
        int port = pHost.port > 0 ? pHost.port : -1; // -1 if not specified, JSch might handle wildcard.

        List<KnownSshHostKey> dbKeys;
        if (port != -1 && type != null) {
            dbKeys = Collections.singletonList(knownSshHostKeyRepository.findByHostnameAndPortAndKeyType(pHost.hostname, port, type).orElse(null));
        } else if (port != -1) {
            dbKeys = knownSshHostKeyRepository.findByHostnameAndPort(pHost.hostname, port);
        } else { // Hostname only, potentially no port, or type may be null
             // This is a simplification, real known_hosts can have entries without explicit port.
             // For now, we'll assume port is usually implied or part of host string.
             // If searching only by hostname, and type is null, could return many keys.
            dbKeys = knownSshHostKeyRepository.findAll().stream()
                .filter(k -> k.getHostname().equalsIgnoreCase(pHost.hostname) && (type == null || k.getKeyType().equals(type)))
                .collect(java.util.stream.Collectors.toList());
        }

        return dbKeys.stream()
                .filter(Objects::nonNull)
                .map(this::convertToJschHostKey)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toArray(HostKey[]::new);
    }

    private Optional<HostKey> convertToJschHostKey(KnownSshHostKey dbKey) {
        if (dbKey == null) return Optional.empty();
        try {
            String hostString = dbKey.getHostname();
            // JSch HostKey constructor can take "[host]:port" or just "host"
            // If port is standard 22, it's often omitted in known_hosts file lines JSch parses.
            // Here, we have port explicitly.
            if (dbKey.getPort() > 0 && dbKey.getPort() != 22) { // Non-standard port
                 hostString = "[" + dbKey.getHostname() + "]:" + dbKey.getPort();
            } else {
                 hostString = dbKey.getHostname(); // Standard port or just hostname
            }

            // Type needs to be passed correctly, HostKey uses it internally.
            // The key itself is the public key bytes.
            return Optional.of(new HostKey(hostString, HostKey.GUESS, Base64.getDecoder().decode(dbKey.getHostKeyBase64())));
        } catch (JSchException e) {
            log.error("Failed to convert KnownSshHostKey (ID: {}) to JSch HostKey: {}", dbKey.getId(), e.getMessage());
            return Optional.empty();
        }
    }

    // Helper record for parsing host string
    private record ParsedHost(String hostname, int port) {}

}
