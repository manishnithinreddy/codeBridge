# CodeBridge Platform

## Overview

CodeBridge is a comprehensive platform for managing development workflows, integrating with various tools and services, and streamlining the development process.

## Architecture

The CodeBridge platform follows a microservices architecture, with each service responsible for a specific domain. The architecture has been optimized to balance modularity with operational simplicity.

### Services

#### Core Services (Remain Separate)

1. **GitLab Service**
   - Version control integration
   - Repository management
   - Git provider configuration
   - Webhook management
   - Issue tracking and management
   - Repository access control
   - Git credentials management
   - Provider type support (GitLab, GitHub, etc.)
   - Issue comments and management
   - Multiple provider support

2. **Docker Service**
   - Docker registry management
   - Container lifecycle management (start, stop, restart)
   - Image management and tagging
   - Registry connection testing
   - Container logs streaming
   - Container monitoring
   - Multi-registry support
   - Access control and security
   - Docker context management

3. **Server Service**
   - Server infrastructure management
   - SSH key management
   - Server monitoring
   - Infrastructure configuration
   - Access control
   - Server provisioning
   - Security management
   - Multiple provider support (AWS, Azure, GCP, Digital Ocean, On-Premise)
   - Team-based server access

4. **API Test Service**
   - API endpoint testing and validation
   - Test case management with name, description, and validation scripts
   - HTTP method support (GET, POST, PUT, DELETE, etc.)
   - Request/response validation with expected status codes and response bodies
   - Custom validation scripts for complex validations
   - Team-based test management
   - Timeout configuration
   - Test execution and results tracking
   - User-specific test management

#### Consolidated Services

5. **Identity Service** (Consolidation of Auth Gateway and Security Service)
   - User authentication and authorization
   - JWT token generation and validation
   - Session management
   - Role-based access control
   - Multi-device session support
   - Token refresh mechanism
   - Complete and device-specific logout
   - Security auditing

6. **Organization Service** (Consolidation of User Management Service and Teams Service)
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

7. **Events Service** (Consolidation of Webhook Service and Audit Service)
   - Webhook event handling
   - Event processing and routing
   - Event status tracking
   - Retry mechanism
   - Event type filtering
   - Signature validation
   - IP filtering
   - Audit logging
   - Security monitoring
   - User activity tracking
   - Service activity logging
   - Error tracking
   - Request/response logging
   - IP and user agent tracking
   - Metadata capture

## Service Communication

Services communicate with each other through:

1. **REST APIs**: Synchronous communication between services
2. **Service Discovery**: Using Eureka for service registration and discovery
3. **Event-Based Communication**: Asynchronous communication for certain operations

## Benefits of Consolidation

The consolidation of services provides several benefits:

1. **Reduced Operational Overhead**
   - Fewer services to deploy, monitor, and maintain
   - Simplified infrastructure requirements

2. **Improved Data Consistency**
   - Fewer distributed transactions across service boundaries
   - Reduced need for complex data synchronization

3. **Simplified Development**
   - Clearer boundaries for developers
   - Fewer inter-service dependencies to manage

4. **Lower Latency**
   - Fewer network hops for common operations
   - Reduced communication overhead

## Technologies

- **Spring Boot**: Framework for building microservices
- **Spring Cloud**: Tools for building cloud-native applications
- **Spring Data JPA**: Data access layer
- **Spring Security**: Security framework
- **PostgreSQL**: Relational database
- **Eureka**: Service discovery
- **Feign**: Declarative REST client
- **JWT**: JSON Web Tokens for authentication
- **Flyway**: Database migration
- **Lombok**: Reduces boilerplate code
- **Maven**: Build tool

## Getting Started

### Prerequisites

- Java 21
- Maven
- PostgreSQL
- Docker (optional)

### Building the Services

To build all services:

```bash
mvn clean install
```

### Running the Services

Each service can be run independently:

```bash
cd codebridge-identity-service
mvn spring-boot:run
```

### Docker Compose

A Docker Compose file is provided to run all services together:

```bash
docker-compose up -d
```

## Configuration

Each service can be configured using environment variables or application.yml files. See the README.md file in each service directory for specific configuration options.
