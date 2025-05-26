-- Create extension for UUID generation
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create audit_log table
CREATE TABLE audit_log (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    audit_id VARCHAR(255) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    type VARCHAR(50) NOT NULL,
    service_name VARCHAR(100) NOT NULL,
    path VARCHAR(255),
    method VARCHAR(10),
    user_id VARCHAR(255),
    status VARCHAR(50),
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create index on audit_id for faster lookups
CREATE INDEX idx_audit_log_audit_id ON audit_log(audit_id);

-- Create index on timestamp for faster queries
CREATE INDEX idx_audit_log_timestamp ON audit_log(timestamp);

-- Create index on user_id for user-specific queries
CREATE INDEX idx_audit_log_user_id ON audit_log(user_id);

-- Create rate_limit table for custom rate limiting
CREATE TABLE rate_limit (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    key VARCHAR(255) NOT NULL,
    tokens INTEGER NOT NULL,
    last_refill TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create unique index on key
CREATE UNIQUE INDEX idx_rate_limit_key ON rate_limit(key);

-- Create user_context table for storing user context
CREATE TABLE user_context (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id VARCHAR(255) NOT NULL,
    active_team_id VARCHAR(255),
    preferences JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create unique index on user_id
CREATE UNIQUE INDEX idx_user_context_user_id ON user_context(user_id);

