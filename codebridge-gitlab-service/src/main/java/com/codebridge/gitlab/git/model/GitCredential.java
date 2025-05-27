package com.codebridge.git.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "git_credentials")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GitCredential extends BaseEntity {

    public enum CredentialType {
        PERSONAL_ACCESS_TOKEN,
        OAUTH_TOKEN,
        SSH_KEY,
        USERNAME_PASSWORD
    }

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "team_id")
    private UUID teamId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CredentialType type;

    @Column(name = "username")
    private String username;

    @Column(name = "token", columnDefinition = "TEXT")
    private String token;

    @Column(name = "password", columnDefinition = "TEXT")
    private String password;

    @Column(name = "ssh_private_key", columnDefinition = "TEXT")
    private String sshPrivateKey;

    @Column(name = "ssh_public_key", columnDefinition = "TEXT")
    private String sshPublicKey;

    @Column(name = "ssh_passphrase", columnDefinition = "TEXT")
    private String sshPassphrase;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "is_default")
    private boolean isDefault;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private GitProvider provider;
}

