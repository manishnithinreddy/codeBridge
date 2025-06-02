package com.codebridge.session.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID; // Using UUID for ID

@Entity
@Table(name = "known_ssh_host_keys",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"hostname", "port", "key_type"}, name = "uk_hostname_port_keytype")
       })
public class KnownSshHostKey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Column(nullable = false)
    private String hostname;

    @NotNull
    @Column(nullable = false)
    private int port;

    @NotBlank
    @Column(name = "key_type", nullable = false) // e.g., "ssh-rsa", "ssh-ed25519"
    private String keyType;

    @NotBlank
    @Lob // For potentially long keys
    @Column(name = "host_key_base64", nullable = false, columnDefinition = "TEXT")
    private String hostKeyBase64; // Base64 encoded public host key

    @Column(name = "fingerprint_sha256", unique = true) // Fingerprint should also be unique if present
    private String fingerprintSha256;

    @NotNull
    @Column(name = "first_seen", nullable = false, updatable = false)
    private LocalDateTime firstSeen;

    @NotNull
    @Column(name = "last_verified", nullable = false)
    private LocalDateTime lastVerified;

    // JPA default constructor
    public KnownSshHostKey() {
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getKeyType() {
        return keyType;
    }

    public void setKeyType(String keyType) {
        this.keyType = keyType;
    }

    public String getHostKeyBase64() {
        return hostKeyBase64;
    }

    public void setHostKeyBase64(String hostKeyBase64) {
        this.hostKeyBase64 = hostKeyBase64;
    }

    public String getFingerprintSha256() {
        return fingerprintSha256;
    }

    public void setFingerprintSha256(String fingerprintSha256) {
        this.fingerprintSha256 = fingerprintSha256;
    }

    public LocalDateTime getFirstSeen() {
        return firstSeen;
    }

    public void setFirstSeen(LocalDateTime firstSeen) {
        this.firstSeen = firstSeen;
    }

    public LocalDateTime getLastVerified() {
        return lastVerified;
    }

    public void setLastVerified(LocalDateTime lastVerified) {
        this.lastVerified = lastVerified;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KnownSshHostKey that = (KnownSshHostKey) o;
        return port == that.port &&
               Objects.equals(id, that.id) &&
               Objects.equals(hostname, that.hostname) &&
               Objects.equals(keyType, that.keyType) &&
               Objects.equals(hostKeyBase64, that.hostKeyBase64) &&
               Objects.equals(fingerprintSha256, that.fingerprintSha256);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, hostname, port, keyType, hostKeyBase64, fingerprintSha256);
    }

    @PrePersist
    protected void onCreate() {
        if (firstSeen == null) {
            firstSeen = LocalDateTime.now();
        }
        if (lastVerified == null) {
            lastVerified = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        // lastVerified should be explicitly set by service logic when a key is successfully used
    }
}
