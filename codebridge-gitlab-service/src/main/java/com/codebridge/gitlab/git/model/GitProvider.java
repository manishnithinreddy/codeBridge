package com.codebridge.gitlab.git.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "git_providers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GitProvider extends BaseEntity {

    public enum ProviderType {
        GITHUB,
        GITLAB,
        BITBUCKET,
        AZURE_DEVOPS,
        GITEA,
        CUSTOM
    }

    @Column(nullable = false, unique = true)
    private String name;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProviderType type;

    @Column(name = "base_url", nullable = false)
    private String baseUrl;

    @Column(name = "api_url", nullable = false)
    private String apiUrl;

    @Column(name = "icon_url")
    private String iconUrl;

    @Column(name = "documentation_url")
    private String documentationUrl;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "supports_oauth", nullable = false)
    private boolean supportsOAuth;

    @Column(name = "supports_pat", nullable = false)
    private boolean supportsPAT;

    @Column(name = "supports_ssh", nullable = false)
    private boolean supportsSSH;

    @Column(name = "supports_webhooks", nullable = false)
    private boolean supportsWebhooks;

    @Column(name = "oauth_client_id")
    private String oauthClientId;

    @Column(name = "oauth_client_secret")
    private String oauthClientSecret;

    @Column(name = "oauth_redirect_uri")
    private String oauthRedirectUri;

    @Column(name = "oauth_scopes")
    private String oauthScopes;

    @Column(name = "api_version")
    private String apiVersion;

    @OneToMany(mappedBy = "provider", fetch = FetchType.LAZY)
    private Set<Repository> repositories = new HashSet<>();
}
