package com.codebridge.git.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "repositories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Repository extends BaseEntity {

    public enum VisibilityType {
        PUBLIC,
        PRIVATE,
        INTERNAL
    }

    @Column(nullable = false)
    private String name;

    @Column(name = "display_name")
    private String displayName;

    @Column(length = 1000)
    private String description;

    @Column(name = "remote_id", nullable = false)
    private String remoteId;

    @Column(name = "remote_url", nullable = false)
    private String remoteUrl;

    @Column(name = "clone_url", nullable = false)
    private String cloneUrl;

    @Column(name = "ssh_url")
    private String sshUrl;

    @Column(name = "web_url", nullable = false)
    private String webUrl;

    @Column(name = "default_branch", nullable = false)
    private String defaultBranch;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VisibilityType visibility;

    @Column(name = "team_id")
    private UUID teamId;

    @Column(name = "owner_name")
    private String ownerName;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "fork_count")
    private Integer forkCount;

    @Column(name = "star_count")
    private Integer starCount;

    @Column(name = "watch_count")
    private Integer watchCount;

    @Column(name = "is_fork")
    private boolean fork;

    @Column(name = "is_archived")
    private boolean archived;

    @Column(name = "is_template")
    private boolean template;

    @Column(name = "last_synced_at")
    private java.time.LocalDateTime lastSyncedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private GitProvider provider;

    @OneToMany(mappedBy = "repository", fetch = FetchType.LAZY)
    private Set<Webhook> webhooks = new HashSet<>();
}

