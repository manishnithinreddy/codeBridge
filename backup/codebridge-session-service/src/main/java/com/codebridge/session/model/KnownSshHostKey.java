package com.codebridge.session.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "known_ssh_host_keys",
    uniqueConstraints = @UniqueConstraint(columnNames = {"hostname", "port", "key_type"}, name = "uk_hostname_port_keytype")
)
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
    @Lob
    @Column(name = "host_key_base64", nullable = false, columnDefinition = "TEXT")
    private String hostKeyBase64; // The actual public key material, Base64 encoded

    @NotBlank
    @Column(name = "fingerprint_sha256", nullable = false, unique = true) // Fingerprints should be unique
    private String fingerprintSha256;

    @CreationTimestamp
    @Column(name = "first_seen", nullable = false, updatable = false)
    private LocalDateTime firstSeen;

    @UpdateTimestamp // Will update when entity is verified and saved again
    @Column(name = "last_verified", nullable = false)
    private LocalDateTime lastVerified;

    // Constructors
    public KnownSshHostKey() {}

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getHostname() { return hostname; }
    public void setHostname(String hostname) { this.hostname = hostname; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public String getKeyType() { return keyType; }
    public void setKeyType(String keyType) { this.keyType = keyType; }
    public String getHostKeyBase64() { return hostKeyBase64; }
    public void setHostKeyBase64(String hostKeyBase64) { this.hostKeyBase64 = hostKeyBase64; }
    public String getFingerprintSha256() { return fingerprintSha256; }
    public void setFingerprintSha256(String fingerprintSha256) { this.fingerprintSha256 = fingerprintSha256; }
    public LocalDateTime getFirstSeen() { return firstSeen; }
    public void setFirstSeen(LocalDateTime firstSeen) { this.firstSeen = firstSeen; }
    public LocalDateTime getLastVerified() { return lastVerified; }
    public void setLastVerified(LocalDateTime lastVerified) { this.lastVerified = lastVerified; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KnownSshHostKey that = (KnownSshHostKey) o;
        return Objects.equals(hostname, that.hostname) &&
               port == that.port &&
               Objects.equals(keyType, that.keyType) &&
               Objects.equals(hostKeyBase64, that.hostKeyBase64); // Key material is most important for equality
    }

    @Override
    public int hashCode() {
        return Objects.hash(hostname, port, keyType, hostKeyBase64);
    }
}
