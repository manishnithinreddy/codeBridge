-- Service Definitions
CREATE TABLE service_definitions (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    url VARCHAR(255) NOT NULL,
    context_path VARCHAR(255),
    scan BOOLEAN NOT NULL,
    enabled BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- API Versions
CREATE TABLE api_versions (
    id UUID PRIMARY KEY,
    service_id UUID NOT NULL REFERENCES service_definitions(id),
    name VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE (service_id, name)
);

-- API Documentation
CREATE TABLE api_documentation (
    id UUID PRIMARY KEY,
    service_id UUID NOT NULL REFERENCES service_definitions(id),
    version_id UUID NOT NULL REFERENCES api_versions(id),
    format VARCHAR(50) NOT NULL,
    open_api_spec TEXT NOT NULL,
    open_api_path VARCHAR(255),
    html_path VARCHAR(255),
    markdown_path VARCHAR(255),
    publish_status VARCHAR(50),
    publish_error VARCHAR(255),
    last_published_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE (service_id, version_id)
);

-- Publish Targets
CREATE TABLE publish_targets (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    type VARCHAR(50) NOT NULL,
    url VARCHAR(255) NOT NULL,
    username VARCHAR(255),
    password VARCHAR(255),
    enabled BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- Code Samples
CREATE TABLE code_samples (
    id UUID PRIMARY KEY,
    documentation_id UUID NOT NULL REFERENCES api_documentation(id),
    operation_id VARCHAR(255) NOT NULL,
    path VARCHAR(255) NOT NULL,
    method VARCHAR(50) NOT NULL,
    language VARCHAR(50) NOT NULL,
    code TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE (documentation_id, operation_id, language)
);

-- API Examples
CREATE TABLE api_examples (
    id UUID PRIMARY KEY,
    documentation_id UUID NOT NULL REFERENCES api_documentation(id),
    operation_id VARCHAR(255) NOT NULL,
    path VARCHAR(255) NOT NULL,
    method VARCHAR(50) NOT NULL,
    request_example TEXT,
    response_example TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE (documentation_id, operation_id)
);

-- Search Index
CREATE TABLE search_index (
    id UUID PRIMARY KEY,
    documentation_id UUID NOT NULL REFERENCES api_documentation(id),
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255),
    description TEXT,
    content TEXT NOT NULL,
    path VARCHAR(255),
    method VARCHAR(50),
    created_at TIMESTAMP NOT NULL
);

-- Create indexes for search
CREATE INDEX idx_search_index_content ON search_index USING gin (content gin_trgm_ops);
CREATE INDEX idx_search_index_title ON search_index USING gin (title gin_trgm_ops);
CREATE INDEX idx_search_index_description ON search_index USING gin (description gin_trgm_ops);

-- Create extension for text search
CREATE EXTENSION IF NOT EXISTS pg_trgm;

