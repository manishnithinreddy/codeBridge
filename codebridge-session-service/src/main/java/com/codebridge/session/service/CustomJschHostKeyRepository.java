package com.codebridge.session.service;

import com.codebridge.session.model.SshHostKey;
import com.codebridge.session.repository.SshHostKeyRepository;
import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.HostKeyRepository;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.UserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Custom implementation of JSch's HostKeyRepository that stores host keys in a database.
 */
@Component
public class CustomJschHostKeyRepository implements HostKeyRepository {

    private static final Logger logger = LoggerFactory.getLogger(CustomJschHostKeyRepository.class);
    private static final Pattern HOST_PORT_PATTERN = Pattern.compile("\\[?([^\\]]+)\\]?(?::(\\d+))?");

    /**
     * Host key verification policy.
     */
    public enum HostKeyVerificationPolicy {
        /**
         * Automatically accept new host keys.
         */
        AUTO_ACCEPT,
        
        /**
         * Prompt the user to accept new host keys.
         */
        PROMPT,
        
        /**
         * Reject new host keys.
         */
        REJECT
    }

    private final SshHostKeyRepository sshHostKeyRepository;
    private UUID userId;
    private JSch jsch;
    private HostKeyVerificationPolicy verificationPolicy = HostKeyVerificationPolicy.AUTO_ACCEPT;

    /**
     * Constructor.
     *
     * @param sshHostKeyRepository the SSH host key repository
     */
    public CustomJschHostKeyRepository(SshHostKeyRepository sshHostKeyRepository) {
        this.sshHostKeyRepository = sshHostKeyRepository;
    }

    /**
     * Constructor.
     *
     * @param sshHostKeyRepository the SSH host key repository
     * @param userId the user ID
     */
    public CustomJschHostKeyRepository(SshHostKeyRepository sshHostKeyRepository, UUID userId) {
        this.sshHostKeyRepository = sshHostKeyRepository;
        this.userId = userId;
    }

    /**
     * Set the user ID.
     *
     * @param userId the user ID
     */
    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    /**
     * Set the JSch instance.
     *
     * @param jsch the JSch instance
     */
    public void setJSch(JSch jsch) {
        this.jsch = jsch;
    }

    /**
     * Set the verification policy.
     *
     * @param policy the verification policy
     */
    public void setVerificationPolicy(HostKeyVerificationPolicy policy) {
        this.verificationPolicy = policy;
    }
    
    /**
     * Get the verification policy.
     *
     * @return the verification policy
     */
    public HostKeyVerificationPolicy getVerificationPolicy() {
        return this.verificationPolicy;
    }

    @Override
    public int check(String host, byte[] key) {
        String[] hostAndPort = parseHostAndPort(host);
        String hostname = hostAndPort[0];
        int port = Integer.parseInt(hostAndPort[1]);

        List<SshHostKey> existingKeys = sshHostKeyRepository.findByUserIdAndHostAndPort(userId, hostname, port);
        if (existingKeys.isEmpty()) {
            logger.debug("No host key found for {}:{} in repository", hostname, port);
            return NOT_INCLUDED;
        }

        String keyBase64 = Base64.getEncoder().encodeToString(key);
        for (SshHostKey existingKey : existingKeys) {
            if (existingKey.getKey().equals(keyBase64)) {
                logger.debug("Host key match found for {}:{}", hostname, port);
                return OK;
            }
        }

        logger.warn("Host key for {}:{} has changed!", hostname, port);
        return CHANGED;
    }

    @Override
    public void add(HostKey hostkey, UserInfo ui) {
        try {
            String[] hostAndPort = parseHostAndPort(hostkey.getHost());
            String hostname = hostAndPort[0];
            int port = Integer.parseInt(hostAndPort[1]);
            String keyType = hostkey.getType();
            String keyBase64 = hostkey.getKey();

            SshHostKey sshHostKey = new SshHostKey();
            sshHostKey.setUserId(userId);
            sshHostKey.setHost(hostname);
            sshHostKey.setPort(port);
            sshHostKey.setKeyType(keyType);
            sshHostKey.setKey(keyBase64);

            sshHostKeyRepository.save(sshHostKey);
            logger.info("Added host key for {}:{} of type {}", hostname, port, keyType);
        } catch (Exception e) {
            logger.error("Failed to add host key: {}", e.getMessage(), e);
        }
    }

    @Override
    public void remove(String host, String type) {
        String[] hostAndPort = parseHostAndPort(host);
        String hostname = hostAndPort[0];
        int port = Integer.parseInt(hostAndPort[1]);

        Optional<SshHostKey> existingKey = sshHostKeyRepository.findByUserIdAndHostAndPortAndKeyType(
                userId, hostname, port, type);

        existingKey.ifPresent(key -> {
            sshHostKeyRepository.delete(key);
            logger.info("Removed host key for {}:{} of type {}", hostname, port, type);
        });
    }

    @Override
    public void remove(String host, String type, byte[] key) {
        // This implementation is simplified - we just use the host and type
        remove(host, type);
    }

    @Override
    public HostKey[] getHostKey() {
        List<SshHostKey> allKeys = sshHostKeyRepository.findByUserId(userId);
        return convertToHostKeys(allKeys);
    }

    @Override
    public HostKey[] getHostKey(String host, String type) {
        String[] hostAndPort = parseHostAndPort(host);
        String hostname = hostAndPort[0];
        int port = Integer.parseInt(hostAndPort[1]);

        List<SshHostKey> keys;
        if (StringUtils.hasText(type)) {
            Optional<SshHostKey> key = sshHostKeyRepository.findByUserIdAndHostAndPortAndKeyType(
                    userId, hostname, port, type);
            keys = key.map(List::of).orElse(List.of());
        } else {
            keys = sshHostKeyRepository.findByUserIdAndHostAndPort(userId, hostname, port);
        }

        return convertToHostKeys(keys);
    }

    @Override
    public String getKnownHostsRepositoryID() {
        return "CustomJschHostKeyRepository-" + userId;
    }

    private String[] parseHostAndPort(String host) {
        Matcher matcher = HOST_PORT_PATTERN.matcher(host);
        if (matcher.matches()) {
            String hostname = matcher.group(1);
            String portStr = matcher.group(2);
            int port = portStr != null ? Integer.parseInt(portStr) : 22;
            return new String[]{hostname, String.valueOf(port)};
        }
        return new String[]{host, "22"};
    }

    private HostKey[] convertToHostKeys(List<SshHostKey> sshHostKeys) {
        List<HostKey> result = new ArrayList<>();
        for (SshHostKey sshHostKey : sshHostKeys) {
            try {
                String hostWithPort = sshHostKey.getHost() + ":" + sshHostKey.getPort();
                byte[] keyBytes = Base64.getDecoder().decode(sshHostKey.getKey());
                
                int keyType;
                switch (sshHostKey.getKeyType()) {
                    case "ssh-rsa":
                        keyType = HostKey.SSHRSA;
                        break;
                    case "ssh-dss":
                        keyType = HostKey.SSHDSS;
                        break;
                    case "ecdsa-sha2-nistp256":
                        keyType = HostKey.ECDSA256;
                        break;
                    case "ecdsa-sha2-nistp384":
                        keyType = HostKey.ECDSA384;
                        break;
                    case "ecdsa-sha2-nistp521":
                        keyType = HostKey.ECDSA521;
                        break;
                    case "ssh-ed25519":
                        keyType = HostKey.ED25519;
                        break;
                    default:
                        keyType = HostKey.GUESS;
                }
                
                HostKey hostKey = new HostKey(hostWithPort, keyType, keyBytes);
                result.add(hostKey);
            } catch (JSchException e) {
                logger.error("Failed to convert SSH host key: {}", e.getMessage(), e);
            }
        }
        return result.toArray(new HostKey[0]);
    }
}

