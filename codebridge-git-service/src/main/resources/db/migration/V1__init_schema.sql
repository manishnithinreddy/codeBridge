-- Git Providers Table
CREATE TABLE git_providers (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(1000),
    type VARCHAR(50) NOT NULL,
    base_url VARCHAR(255) NOT NULL,
    api_url VARCHAR(255) NOT NULL,
    icon_url VARCHAR(255),
    documentation_url VARCHAR(255),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    supports_oauth BOOLEAN NOT NULL DEFAULT FALSE,
    supports_pat BOOLEAN NOT NULL DEFAULT FALSE,
    supports_ssh BOOLEAN NOT NULL DEFAULT FALSE,
    supports_webhooks BOOLEAN NOT NULL DEFAULT FALSE,
    oauth_client_id VARCHAR(255),
    oauth_client_secret VARCHAR(255),
    oauth_redirect_uri VARCHAR(255),
    oauth_scopes VARCHAR(255),
    api_version VARCHAR(50),
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    updated_at TIMESTAMP,
    updated_by VARCHAR(255),
    version BIGINT DEFAULT 0
);

-- Repositories Table
CREATE TABLE repositories (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    display_name VARCHAR(255),
    description VARCHAR(1000),
    remote_id VARCHAR(255) NOT NULL,
    remote_url VARCHAR(255) NOT NULL,
    clone_url VARCHAR(255) NOT NULL,
    ssh_url VARCHAR(255),
    web_url VARCHAR(255) NOT NULL,
    default_branch VARCHAR(255) NOT NULL,
    visibility VARCHAR(50) NOT NULL,
    team_id UUID,
    owner_name VARCHAR(255),
    avatar_url VARCHAR(255),
    fork_count INTEGER,
    star_count INTEGER,
    watch_count INTEGER,
    is_fork BOOLEAN DEFAULT FALSE,
    is_archived BOOLEAN DEFAULT FALSE,
    is_template BOOLEAN DEFAULT FALSE,
    last_synced_at TIMESTAMP,
    provider_id UUID NOT NULL REFERENCES git_providers(id),
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    updated_at TIMESTAMP,
    updated_by VARCHAR(255),
    version BIGINT DEFAULT 0
);

-- Webhooks Table
CREATE TABLE webhooks (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    remote_id VARCHAR(255),
    payload_url VARCHAR(255) NOT NULL,
    secret_token VARCHAR(255),
    content_type VARCHAR(50) NOT NULL,
    events TEXT NOT NULL,
    status VARCHAR(50) NOT NULL,
    last_triggered_at TIMESTAMP,
    last_response_code INTEGER,
    last_response_message TEXT,
    failure_count INTEGER DEFAULT 0,
    repository_id UUID NOT NULL REFERENCES repositories(id),
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    updated_at TIMESTAMP,
    updated_by VARCHAR(255),
    version BIGINT DEFAULT 0
);

-- Git Credentials Table
CREATE TABLE git_credentials (
    id UUID PRIMARY KEY,
    user_id UUID,
    team_id UUID,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    type VARCHAR(50) NOT NULL,
    username VARCHAR(255),
    token TEXT,
    password TEXT,
    ssh_private_key TEXT,
    ssh_public_key TEXT,
    ssh_passphrase TEXT,
    expires_at TIMESTAMP,
    last_used_at TIMESTAMP,
    is_default BOOLEAN DEFAULT FALSE,
    provider_id UUID NOT NULL REFERENCES git_providers(id),
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    updated_at TIMESTAMP,
    updated_by VARCHAR(255),
    version BIGINT DEFAULT 0
);

-- Indexes
CREATE INDEX idx_repositories_provider_id ON repositories(provider_id);
CREATE INDEX idx_repositories_team_id ON repositories(team_id);
CREATE INDEX idx_webhooks_repository_id ON webhooks(repository_id);
CREATE INDEX idx_git_credentials_provider_id ON git_credentials(provider_id);
CREATE INDEX idx_git_credentials_user_id ON git_credentials(user_id);
CREATE INDEX idx_git_credentials_team_id ON git_credentials(team_id);

-- Default Git Providers
INSERT INTO git_providers (
    id, name, description, type, base_url, api_url, 
    icon_url, documentation_url, enabled, 
    supports_oauth, supports_pat, supports_ssh, supports_webhooks,
    created_at
) VALUES (
    '2f9a9c9e-6b5d-4e1a-9f8d-3b7c8e5a9b1c', 
    'GitHub', 
    'GitHub is a web-based hosting service for version control using Git.', 
    'GITHUB', 
    'https://github.com', 
    'https://api.github.com',
    'https://github.githubassets.com/images/modules/logos_page/GitHub-Mark.png',
    'https://docs.github.com/en/rest',
    TRUE,
    TRUE, TRUE, TRUE, TRUE,
    CURRENT_TIMESTAMP
);

INSERT INTO git_providers (
    id, name, description, type, base_url, api_url, 
    icon_url, documentation_url, enabled, 
    supports_oauth, supports_pat, supports_ssh, supports_webhooks,
    created_at
) VALUES (
    '3e8b8d7c-5a4c-3f2b-8e7d-2c1b0a9d8e7f', 
    'GitLab', 
    'GitLab is a web-based DevOps lifecycle tool that provides a Git repository manager.', 
    'GITLAB', 
    'https://gitlab.com', 
    'https://gitlab.com/api/v4',
    'https://about.gitlab.com/images/press/logo/png/gitlab-icon-rgb.png',
    'https://docs.gitlab.com/ee/api/',
    TRUE,
    TRUE, TRUE, TRUE, TRUE,
    CURRENT_TIMESTAMP
);

INSERT INTO git_providers (
    id, name, description, type, base_url, api_url, 
    icon_url, documentation_url, enabled, 
    supports_oauth, supports_pat, supports_ssh, supports_webhooks,
    created_at
) VALUES (
    '4d7c6b5a-3f2e-1d0c-9b8a-7e6d5c4b3a2f', 
    'Bitbucket', 
    'Bitbucket is a Git-based source code repository hosting service owned by Atlassian.', 
    'BITBUCKET', 
    'https://bitbucket.org', 
    'https://api.bitbucket.org/2.0',
    'https://wac-cdn.atlassian.com/assets/img/favicons/bitbucket/favicon-32x32.png',
    'https://developer.atlassian.com/bitbucket/api/2/reference/',
    TRUE,
    TRUE, TRUE, TRUE, TRUE,
    CURRENT_TIMESTAMP
);

