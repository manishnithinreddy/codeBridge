package com.codebridge.gitlab.git.model;

import com.codebridge.gitlab.git.model.BaseEntity;
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "repositories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
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
    
    // Manual getters and setters in case Lombok is not working
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public GitProvider getProvider() {
        return provider;
    }
    
    public void setProvider(GitProvider provider) {
        this.provider = provider;
    }
    
    // Manual getId() and setId() methods since Lombok is not working
    public UUID getId() {
        return super.getId();
    }
    
    public void setId(UUID id) {
        super.setId(id);
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getRemoteUrl() {
        return remoteUrl;
    }
    
    public void setRemoteUrl(String remoteUrl) {
        this.remoteUrl = remoteUrl;
    }
    
    public String getCloneUrl() {
        return cloneUrl;
    }
    
    public void setCloneUrl(String cloneUrl) {
        this.cloneUrl = cloneUrl;
    }
    
    public String getSshUrl() {
        return sshUrl;
    }
    
    public void setSshUrl(String sshUrl) {
        this.sshUrl = sshUrl;
    }
    
    public String getWebUrl() {
        return webUrl;
    }
    
    public void setWebUrl(String webUrl) {
        this.webUrl = webUrl;
    }
    
    public String getDefaultBranch() {
        return defaultBranch;
    }
    
    public void setDefaultBranch(String defaultBranch) {
        this.defaultBranch = defaultBranch;
    }
    
    public VisibilityType getVisibility() {
        return visibility;
    }
    
    public void setVisibility(VisibilityType visibility) {
        this.visibility = visibility;
    }
    
    public Set<Webhook> getWebhooks() {
        return webhooks;
    }
    
    public void setWebhooks(Set<Webhook> webhooks) {
        this.webhooks = webhooks;
    }
}
