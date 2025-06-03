package com.codebridge.session.repository;

import com.codebridge.session.model.KnownSshHostKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface KnownSshHostKeyRepository extends JpaRepository<KnownSshHostKey, UUID> {

    Optional<KnownSshHostKey> findByHostnameAndPortAndKeyType(String hostname, int port, String keyType);

    List<KnownSshHostKey> findByHostnameAndPort(String hostname, int port);

    Optional<KnownSshHostKey> findByFingerprintSha256(String fingerprintSha256);
}
