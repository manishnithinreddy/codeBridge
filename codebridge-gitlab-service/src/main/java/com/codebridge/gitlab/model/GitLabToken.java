package com.codebridge.gitlab.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a GitLab access token.
 */
@Entity
@Table(name = "gitlab_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GitLabToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "team_id")
    private UUID teamId;

    @Column(name = "token_value", nullable = false)
    private String tokenValue;

    @Column(name = "token_name")
    private String tokenName;

    @Column(name = "gitlab_user_id")
    private Long gitlabUserId;

    @Column(name = "gitlab_username")
    private String gitlabUsername;

    @Column(name = "expiration_date")
    private LocalDateTime expirationDate;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

