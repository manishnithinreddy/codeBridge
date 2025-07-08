-- CodeBridge Security Database Setup
-- Run this script to create the database if it doesn't exist

-- Create database (run this as postgres superuser)
CREATE DATABASE codebridge_security;

-- Connect to the database
\c codebridge_security;

-- Create a user for the application (optional, you can use postgres user)
-- CREATE USER codebridge_user WITH PASSWORD 'your_password';
-- GRANT ALL PRIVILEGES ON DATABASE codebridge_security TO codebridge_user;

-- The application will automatically create tables using Hibernate DDL
-- No need to create tables manually as we're using ddl-auto: update

-- Verify database is ready
SELECT 'Database codebridge_security is ready!' as status;
