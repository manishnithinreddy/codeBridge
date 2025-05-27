# CodeBridge User Management Service

This service provides comprehensive user management capabilities for the CodeBridge platform, including:

## Features

- **Authentication & Authorization**
  - User authentication
  - Role-based access control
  - Session management

- **User Profiles**
  - Profile management
  - User preferences
  - Notification preferences

- **Team Management**
  - Team memberships
  - Role assignments
  - Team permissions

- **Application Settings**
  - Global settings
  - Environment-specific configurations

- **Feature Flags**
  - Feature toggle management
  - Gradual rollout support
  - User-specific feature enablement

## Architecture

The service follows a modular architecture with clear separation of concerns:

```
codebridge-user-management-service/
├── src/main/java/com/codebridge/usermanagement/
    ├── auth/
    │   ├── model/
    │   ├── repository/
    │   ├── service/
    │   └── controller/
    ├── session/
    │   ├── model/
    │   ├── repository/
    │   ├── service/
    │   └── controller/
    ├── profile/
    │   ├── model/
    │   ├── repository/
    │   ├── service/
    │   └── controller/
    ├── settings/
    │   ├── model/
    │   ├── repository/
    │   ├── service/
    │   └── controller/
    ├── team/
    │   ├── model/
    │   ├── repository/
    │   ├── service/
    │   └── controller/
    ├── feature/
    │   ├── model/
    │   ├── repository/
    │   ├── service/
    │   └── controller/
    └── common/
        ├── model/
        ├── util/
        ├── exception/
        └── config/
```

## API Documentation

The service exposes RESTful APIs for all functionality. API documentation is available via Swagger UI at `/swagger-ui.html` when the service is running.

## Security

- Implements OAuth2/JWT for authentication
- Role-based access control for authorization
- Input validation and sanitization
- Secure communication with TLS

## Monitoring & Logging

- Structured JSON logging
- Prometheus metrics collection
- Health check endpoints

## Getting Started

### Prerequisites
- Java 17+
- Maven 3.8+
- PostgreSQL 14+

### Running Locally
1. Clone the repository
2. Configure database connection in `application.yml`
3. Run `mvn spring-boot:run`
4. Access the API at `http://localhost:8080`

## Testing

- Unit tests: `mvn test`
- Integration tests: `mvn verify`
- Load tests: `mvn gatling:test`

