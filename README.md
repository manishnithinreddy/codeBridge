# CodeBridge Microservices Consolidation

This project implements the consolidation of CodeBridge microservices while maintaining the independence of key services for scalability.

## Consolidation Summary

### Removed Services (Consolidated)
The following services have been removed as part of the consolidation:

- ~~codebridge-usermanagement-service~~ (consolidated into Identity Platform)
- ~~codebridge-identity-service~~ (consolidated into Identity Platform)
- ~~codebridge-organization-service~~ (consolidated into Identity Platform, except teams functionality)
- ~~codebridge-admin-service~~ (consolidated into Platform Operations)
- ~~codebridge-events-service~~ (consolidated into Platform Operations)

### New Consolidated Services
These new services replace the removed services:

1. **codebridge-identity-platform** - Combines user management, identity, and organization functionality
2. **codebridge-teams-service** - Handles team management (extracted from organization service)
3. **codebridge-platform-ops** - Combines admin dashboard and events functionality

### Independent Services (Unchanged)
These services remain separate for independent scaling:

- codebridge-api-test-service
- codebridge-server-service & codebridge-session-service (for server connections)
- codebridge-ai-db-agent-service
- codebridge-gitlab-service
- codebridge-docker-service

## Architecture Overview

The CodeBridge platform has been restructured to consolidate related microservices while keeping certain key services separate for independent scaling.

### 1. CodeBridge Identity Platform (Port 8081)

Combines the following services:
- usermanagement-service
- identity-service
- organization-service (except teams functionality)

**Key Components:**
- User authentication and authorization
- User profile management
- Organization management
- Role-based access control

**API Endpoints:**
- `/api/auth/*` - Authentication operations
- `/api/users/*` - User management
- `/api/organizations/*` - Organization management

### 2. CodeBridge Teams Service (Port 8082)

Standalone service for team management:
- Team creation and management
- Team membership with roles (MEMBER, ADMIN, OWNER)
- Hierarchical team structure

**API Endpoints:**
- `/api/teams/*` - Team operations
- `/api/teams/{id}/members/*` - Team member management

### 3. CodeBridge Platform Operations (Port 8083)

Combines the following services:
- admin-service
- events-service

**Key Components:**
- Admin dashboard and system monitoring
- Webhook management and event processing
- Audit logging and reporting

**API Endpoints:**
- `/api/admin/dashboard/*` - Admin dashboard operations
- `/api/webhooks/*` - Webhook management
- `/api/audit/*` - Audit log operations

## Security Features

1. JWT-based authentication
2. Role-based access control
3. OAuth2 resource server configuration
4. Method-level security with @PreAuthorize

## Service Discovery

All services register with Eureka:
- Default zone: http://localhost:8761/eureka/
- Instance preference: prefer-ip-address: true

## Monitoring

- Actuator endpoints enabled
- Health, info, metrics, and Prometheus endpoints exposed
- Detailed health information available

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.8 or higher
- Docker (optional, for containerization)

### Running the Services

1. Start the Eureka Server (if not already running)
2. Start the Identity Platform:
   ```
   cd codebridge-identity-platform
   ./mvnw spring-boot:run
   ```

3. Start the Teams Service:
   ```
   cd codebridge-teams-service
   ./mvnw spring-boot:run
   ```

4. Start the Platform Operations Service:
   ```
   cd codebridge-platform-ops
   ./mvnw spring-boot:run
   ```

## API Documentation

API documentation is available at:
- Identity Platform: http://localhost:8081/identity/swagger-ui.html
- Teams Service: http://localhost:8082/teams/swagger-ui.html
- Platform Operations: http://localhost:8083/ops/swagger-ui.html

