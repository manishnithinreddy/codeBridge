-- Initialize databases for CodeBridge services
-- This script creates separate databases for each service

-- Create databases
CREATE DATABASE IF NOT EXISTS codebridge_security;
CREATE DATABASE IF NOT EXISTS codebridge_server;
CREATE DATABASE IF NOT EXISTS codebridge_teams;
CREATE DATABASE IF NOT EXISTS codebridge_monitoring;
CREATE DATABASE IF NOT EXISTS codebridge_documentation;
CREATE DATABASE IF NOT EXISTS codebridge_api_test;
CREATE DATABASE IF NOT EXISTS codebridge_docker;

-- Grant permissions to postgres user (already has superuser privileges)
-- These are redundant but included for clarity
GRANT ALL PRIVILEGES ON DATABASE codebridge_security TO postgres;
GRANT ALL PRIVILEGES ON DATABASE codebridge_server TO postgres;
GRANT ALL PRIVILEGES ON DATABASE codebridge_teams TO postgres;
GRANT ALL PRIVILEGES ON DATABASE codebridge_monitoring TO postgres;
GRANT ALL PRIVILEGES ON DATABASE codebridge_documentation TO postgres;
GRANT ALL PRIVILEGES ON DATABASE codebridge_api_test TO postgres;
GRANT ALL PRIVILEGES ON DATABASE codebridge_docker TO postgres;

-- Create a dedicated user for production (optional)
-- CREATE USER codebridge_user WITH PASSWORD 'codebridge_password';
-- GRANT ALL PRIVILEGES ON DATABASE codebridge_security TO codebridge_user;
-- GRANT ALL PRIVILEGES ON DATABASE codebridge_server TO codebridge_user;
-- GRANT ALL PRIVILEGES ON DATABASE codebridge_teams TO codebridge_user;
-- GRANT ALL PRIVILEGES ON DATABASE codebridge_monitoring TO codebridge_user;
-- GRANT ALL PRIVILEGES ON DATABASE codebridge_documentation TO codebridge_user;
-- GRANT ALL PRIVILEGES ON DATABASE codebridge_api_test TO codebridge_user;
-- GRANT ALL PRIVILEGES ON DATABASE codebridge_docker TO codebridge_user;
