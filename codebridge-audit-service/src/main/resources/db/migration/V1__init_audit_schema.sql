-- Audit logs table
CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY,
    audit_id VARCHAR(255) NOT NULL UNIQUE,
    timestamp TIMESTAMP NOT NULL,
    type VARCHAR(50) NOT NULL,
    service_name VARCHAR(100) NOT NULL,
    path VARCHAR(255),
    method VARCHAR(10),
    user_id UUID,
    team_id UUID,
    status VARCHAR(50),
    error_message VARCHAR(1000),
    request_body TEXT,
    response_body TEXT,
    ip_address VARCHAR(50),
    user_agent VARCHAR(255),
    created_at TIMESTAMP NOT NULL
);

-- Indexes
CREATE INDEX idx_audit_timestamp ON audit_logs(timestamp);
CREATE INDEX idx_audit_type ON audit_logs(type);
CREATE INDEX idx_audit_service ON audit_logs(service_name);
CREATE INDEX idx_audit_user ON audit_logs(user_id);
CREATE INDEX idx_audit_team ON audit_logs(team_id);

