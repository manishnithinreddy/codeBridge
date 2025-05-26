# CodeBridge Microservices Platform

CodeBridge is a comprehensive microservices platform designed to streamline software development workflows. It provides a set of integrated services for team management, authentication, GitLab integration, Docker container management, server infrastructure management, and API testing.

## Architecture Overview

The platform follows a microservices architecture with the following components:

### Core Services

1. **Auth Gateway Service**
   - Central authentication and authorization service
   - JWT token management
   - Role-based access control
   - API routing and gateway functionality

2. **Teams Service**
   - Team and user management
   - Permission management
   - Team-based access control

3. **Audit Service**
   - Immutable audit logging
   - Activity tracking
   - Compliance reporting

### Integration Services

4. **GitLab Integration Service**
   - GitLab API integration
   - Repository management
   - CI/CD pipeline integration
   - Token management

5. **Docker Management Service**
   - Container lifecycle management
   - Image management
   - Container monitoring
   - Resource allocation

6. **Server Management Service**
   - Server provisioning
   - SSH key management
   - Server monitoring
   - Infrastructure management

7. **API Testing Service**
   - API endpoint testing
   - Test automation
   - Test reporting
   - Validation scripting

## Security Features

- JWT-based authentication
- Role-based access control
- Team-based permissions
- Secure credential storage
- Comprehensive audit logging
- Stateless session management

## Technology Stack

- **Framework**: Spring Boot 3.1.x
- **Service Discovery**: Spring Cloud Netflix Eureka
- **Configuration**: Spring Cloud Config
- **Circuit Breaker**: Resilience4j
- **Database**: PostgreSQL
- **Migration**: Flyway
- **Documentation**: SpringDoc OpenAPI
- **Build Tool**: Maven
- **Java Version**: 17

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.8 or higher
- PostgreSQL 14 or higher
- Docker (for containerized deployment)

### Building the Project

```bash
mvn clean install
```

### Running Services

Each service can be run independently:

```bash
cd codebridge-auth-gateway
mvn spring-boot:run
```

### Docker Deployment

```bash
docker-compose up -d
```

## Development Guidelines

- Follow standard Spring Boot practices
- Use DTOs for API requests/responses
- Implement proper exception handling
- Write comprehensive unit and integration tests
- Document APIs using OpenAPI annotations

## License

This project is licensed under the MIT License - see the LICENSE file for details.

