# CodeBridge Identity Service

## Overview

The Identity Service is responsible for authentication, authorization, and session management within the CodeBridge platform. This service consolidates the functionality previously provided by the Auth Gateway and Security Service.

## Features

- User authentication and authorization
- JWT token generation and validation
- Session management
- Role-based access control
- Multi-device session support
- Token refresh mechanism
- Complete and device-specific logout
- Security auditing

## Architecture

The Identity Service follows a layered architecture:

- **Controllers**: Handle HTTP requests and responses
- **Services**: Implement business logic
- **Repositories**: Interact with the database
- **Models**: Represent data entities
- **DTOs**: Transfer data between layers
- **Security**: Configure security settings and filters

## Key Components

### Models

- **User**: Represents a user in the system
- **Role**: Represents a role with specific permissions
- **UserSession**: Tracks user sessions across devices

### Services

- **AuthenticationService**: Handles user authentication and token generation
- **UserService**: Manages user-related operations
- **SessionService**: Manages user sessions
- **RoleService**: Manages roles and permissions

## API Endpoints

The Identity Service exposes the following API endpoints:

- `/api/identity/auth/login`: Authenticate a user and generate tokens
- `/api/identity/auth/refresh`: Refresh an access token
- `/api/identity/auth/logout`: Log out a user from the current device
- `/api/identity/auth/logout-all`: Log out a user from all devices
- `/api/identity/users`: User management endpoints
- `/api/identity/roles`: Role management endpoints
- `/api/identity/sessions`: Session management endpoints

## Configuration

The service can be configured using the following environment variables:

- `DB_HOST`: Database host (default: localhost)
- `DB_PORT`: Database port (default: 5432)
- `DB_NAME`: Database name (default: codebridge_identity)
- `DB_USERNAME`: Database username (default: postgres)
- `DB_PASSWORD`: Database password (default: postgres)
- `SERVER_PORT`: Server port (default: 8081)
- `EUREKA_URI`: Eureka server URI (default: http://localhost:8761/eureka)
- `JWT_SECRET`: Secret key for JWT tokens (default: your-secret-key)
- `JWT_EXPIRATION`: JWT token expiration time in seconds (default: 86400)
- `JWT_REFRESH_EXPIRATION`: JWT refresh token expiration time in seconds (default: 604800)

## Dependencies

- Spring Boot 3.1.0
- Spring Security
- Spring Data JPA
- Spring Cloud Netflix Eureka Client
- PostgreSQL
- JJWT
- Lombok
- Flyway

