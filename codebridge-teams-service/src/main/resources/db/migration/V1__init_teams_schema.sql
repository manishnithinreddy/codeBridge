-- Teams table
CREATE TABLE IF NOT EXISTS teams (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(1000),
    owner_id UUID NOT NULL,
    parent_team_id UUID,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_parent_team FOREIGN KEY (parent_team_id) REFERENCES teams(id)
);

-- Team members table
CREATE TABLE IF NOT EXISTS team_members (
    id UUID PRIMARY KEY,
    team_id UUID NOT NULL,
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_team FOREIGN KEY (team_id) REFERENCES teams(id),
    CONSTRAINT uk_team_user UNIQUE (team_id, user_id)
);

-- Team services table
CREATE TABLE IF NOT EXISTS team_services (
    id UUID PRIMARY KEY,
    team_id UUID NOT NULL,
    service_id UUID NOT NULL,
    access_level VARCHAR(50) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_team_service FOREIGN KEY (team_id) REFERENCES teams(id),
    CONSTRAINT uk_team_service UNIQUE (team_id, service_id)
);

-- Team tokens table
CREATE TABLE IF NOT EXISTS team_tokens (
    id UUID PRIMARY KEY,
    team_id UUID NOT NULL,
    token_name VARCHAR(255) NOT NULL,
    token_value VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_team_token FOREIGN KEY (team_id) REFERENCES teams(id)
);

-- Indexes
CREATE INDEX idx_team_owner ON teams(owner_id);
CREATE INDEX idx_team_parent ON teams(parent_team_id);
CREATE INDEX idx_team_member_team ON team_members(team_id);
CREATE INDEX idx_team_member_user ON team_members(user_id);
CREATE INDEX idx_team_service_team ON team_services(team_id);
CREATE INDEX idx_team_service_service ON team_services(service_id);
CREATE INDEX idx_team_token_team ON team_tokens(team_id);
CREATE INDEX idx_team_token_value ON team_tokens(token_value);

