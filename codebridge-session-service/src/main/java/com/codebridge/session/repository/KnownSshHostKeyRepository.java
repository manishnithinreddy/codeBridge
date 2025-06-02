package com.codebridge.session.repository;

import com.codebridge.session.model.KnownSshHostKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface KnownSshHostKeyRepository extends JpaRepository<KnownSshHostKey, UUID> {

    /**
     * Finds a known host key by its exact hostname, port, and key type.
     * This is the most specific lookup.
     */
    Optional<KnownSshHostKey> findByHostnameAndPortAndKeyType(String hostname, int port, String keyType);

    /**
     * Finds all known host keys for a given hostname and port combination.
     * This is useful when a host might present a different key type than what's stored,
     * or if a key has changed and we need to detect it.
     */
    List<KnownSshHostKey> findByHostnameAndPort(String hostname, int port);

    /**
     * Finds a known host key by its SHA256 fingerprint.
     * This can be useful if the hostname/port changes (e.g. dynamic IP) but the key remains the same.
     * Note: fingerprintSha256 must be unique in the DB for this to be reliable.
     */
    Optional<KnownSshHostKey> findByFingerprintSha256(String fingerprintSha256);
}
