package com.codebridge.session.repository;

import com.codebridge.session.model.SshHostKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for SSH host keys.
 */
@Repository
public interface SshHostKeyRepository extends JpaRepository<SshHostKey, UUID> {

    /**
     * Find all host keys for a user.
     *
     * @param userId the user ID
     * @return the list of host keys
     */
    List<SshHostKey> findByUserId(UUID userId);

    /**
     * Find all host keys for a user and host.
     *
     * @param userId the user ID
     * @param host the host
     * @return the list of host keys
     */
    List<SshHostKey> findByUserIdAndHost(UUID userId, String host);

    /**
     * Find all host keys for a user, host, and port.
     *
     * @param userId the user ID
     * @param host the host
     * @param port the port
     * @return the list of host keys
     */
    List<SshHostKey> findByUserIdAndHostAndPort(UUID userId, String host, int port);

    /**
     * Find a host key for a user, host, port, and key type.
     *
     * @param userId the user ID
     * @param host the host
     * @param port the port
     * @param keyType the key type
     * @return the host key, if found
     */
    Optional<SshHostKey> findByUserIdAndHostAndPortAndKeyType(UUID userId, String host, int port, String keyType);
}

