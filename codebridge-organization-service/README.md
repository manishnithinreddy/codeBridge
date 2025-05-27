# CodeBridge Organization Service

## Overview

The Organization Service is responsible for user and team management within the CodeBridge platform. This service consolidates the functionality previously provided by the User Management Service and Teams Service.

## Features

- User profile management
- Team management
- Team hierarchy (parent/child teams)
- Team membership
- Role assignment
- User preferences and settings
- Feature flag management
- GitLab issue integration
- Team-based access control
- Team collaboration features

## Architecture

The Organization Service follows a layered architecture:

- **Controllers**: Handle HTTP requests and responses
- **Services**: Implement business logic
- **Repositories**: Interact with the database
- **Models**: Represent data entities
- **DTOs**: Transfer data between layers
- **Clients**: Communicate with other services

## Key Components

### Models

- **Team**: Represents a team with hierarchical structure
- **TeamMember**: Links users to teams with specific roles
- **UserProfile**: Contains extended information about a user

### Services

- **TeamService**: Manages team-related operations
- **TeamMemberService**: Manages team membership
- **UserProfileService**: Manages user profiles and preferences
- **GitLabIntegrationService**: Handles GitLab issue integration

## API Endpoints

The Organization Service exposes the following API endpoints:

- `/api/organization/teams`: Team management endpoints
- `/api/organization/teams/{teamId}/members`: Team membership endpoints
- `/api/organization/users/profiles`: User profile management endpoints
- `/api/organization/users/preferences`: User preference management endpoints
- `/api/organization/users/feature-flags`: Feature flag management endpoints

## Configuration

The service can be configured using the following environment variables:

- `DB_HOST`: Database host (default: localhost)
- `DB_PORT`: Database port (default: 5432)
- `DB_NAME`: Database name (default: codebridge_organization)
- `DB_USERNAME`: Database username (default: postgres)
- `DB_PASSWORD`: Database password (default: postgres)
- `SERVER_PORT`: Server port (default: 8082)
- `EUREKA_URI`: Eureka server URI (default: http://localhost:8761/eureka)

## Dependencies

- Spring Boot 3.1.0
- Spring Data JPA
- Spring Cloud Netflix Eureka Client
- Spring Cloud OpenFeign
- Spring Cache with Caffeine
- PostgreSQL
- Lombok
- Flyway

