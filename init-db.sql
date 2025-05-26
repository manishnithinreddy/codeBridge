-- Create databases
CREATE DATABASE codebridge_git;
CREATE DATABASE codebridge_docker;
CREATE DATABASE codebridge_server;
CREATE DATABASE codebridge_api;

-- Connect to each database and create extensions
\c codebridge_git
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

\c codebridge_docker
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

\c codebridge_server
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

\c codebridge_api
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

