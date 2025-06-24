# CodeBridge Platform

CodeBridge is a comprehensive development platform that integrates various development tools and services into a unified ecosystem. It provides a unified interface for managing code repositories, CI/CD pipelines, containers, databases, documentation, and team collaboration.

## Architecture Overview

The CodeBridge platform follows a microservices architecture with the following components:

### Core Infrastructure

- **Gateway Service** (`codebridge-gateway-service`): Centralized entry point for all client requests, handling routing, load balancing, and service discovery.
- **Identity Platform** (`codebridge-identity-platform`): User identity management, authentication, and SSO.
- **Security** (`codebridge-security`): Security components, authentication, and authorization.

### Development Tools

- **GitLab Service** (`codebridge-gitlab-service`): Integration with GitLab for project management, pipelines, and jobs.
- **Docker Service** (`codebridge-docker-service`): Integration with Docker for container management, images, and registries.
- **Documentation Service** (`codebridge-documentation-service`): API documentation generation, versioning, and publishing.

### Database and AI Services

- **DB Service** (`db-service`): Go implementation for database connection management and query execution.
- **AI Service** (`ai-service`): Python implementation for AI-powered database interactions.

### Session and Server Management

- **Session Service** (`session-service`): Go implementation for session management.
- **Server Service** (`codebridge-server-service`): Server provisioning and management.

### Team Collaboration

- **Teams Service** (`codebridge-teams-service`): Team management and collaboration.

### Monitoring and Performance

- **Monitoring Service** (`codebridge-monitoring-service`): Consolidated monitoring service including performance monitoring.

## Technology Stack

- **Java**: Primary implementation language for most services (Spring Boot)
- **Go**: Implementation for performance-critical services (DB Service, Session Service)
- **Python**: Implementation for AI-related services
- **Redis**: For caching and session storage
- **PostgreSQL**: For persistent storage
- **Docker & Kubernetes**: For containerization and orchestration

## Getting Started

### Prerequisites

- Docker and Docker Compose
- Java 17 or higher
- Go 1.21 or higher
- Python 3.10 or higher

### Running with Docker Compose

The easiest way to run the platform is using Docker Compose:

```bash
docker-compose up -d
```

This will start all services and their dependencies.

### Building and Running Manually

To build and run the services manually, refer to the README.md file in each service directory.

## Service Endpoints

- Gateway Service: http://localhost:8080
- GitLab Service: http://localhost:8081/api/gitlab
- Docker Service: http://localhost:8082/api/docker
- Session Service: http://localhost:8083/api/session
- DB Service: http://localhost:8084/api/db
- AI Service: http://localhost:8085/api/ai
- Documentation Service: http://localhost:8087/api/docs
- Monitoring Service: http://localhost:8088/monitoring
- Teams Service: http://localhost:8089/teams
- Identity Platform: http://localhost:8090/identity

## API Documentation

Each service provides Swagger/OpenAPI documentation at its `/swagger-ui.html` endpoint.

## Multi-Language Implementation Strategy

The platform employs a polyglot strategy with services implemented in different languages:

- **Java**: Primary implementation language for most services
- **Go**: Implementation for performance-critical services (DB Service, Session Service)
- **Python**: Implementation for AI-related services

This strategy allows for:
1. Optimizing performance-critical services with Go
2. Leveraging Python's strengths for AI and machine learning
3. Using Java's ecosystem for enterprise features
