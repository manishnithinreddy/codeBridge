# CodeBridge Core

This module serves as the core foundation for the CodeBridge platform, providing essential infrastructure components, models, repositories, and utilities used by other services.

## Features

### Core Infrastructure
- Base entity models and repositories
- Common DTOs and mappers
- Exception handling and error responses
- Security utilities and configurations
- Tracing and observability
- Rate limiting and circuit breaking
- Caching configuration

### Team and User Management
- User, Team, and Role models
- Repository interfaces for entity access
- Mappers for DTO conversion
- Service interfaces for business logic

### Security Features
- JWT token validation
- Method-level security
- Secret storage service
- User principal management
- Keycloak integration

### Resilience Patterns
- Circuit breaker implementation
- Rate limiting
- Async configuration
- Compression utilities

## Architecture

The module follows a clean architecture approach:
- **Model Layer**: Domain models and entities
- **Repository Layer**: Data access interfaces
- **Service Layer**: Business logic
- **Configuration**: Application configuration
- **Security**: Security-related components
- **Exception Handling**: Global exception handling

## Integration

This module is designed to be included as a dependency in other CodeBridge services, providing a consistent foundation for all services in the platform.

## Dependencies

- Spring Boot
- Spring Security
- Spring Data JPA
- Lombok
- MapStruct
- Resilience4j

