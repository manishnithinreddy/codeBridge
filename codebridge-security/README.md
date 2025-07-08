# CodeBridge Security Platform

A comprehensive security platform for the CodeBridge ecosystem, providing authentication, authorization, API security, and identity management capabilities.

## Features

### Authentication
- User registration and login
- JWT token generation and validation
- Refresh token handling
- Multi-factor authentication
- Session management

### Authorization
- Role-based access control (RBAC)
- Permission management
- Dynamic authorization rules
- User-role assignment

### API Security
- API key management
- Rate limiting
- IP restrictions
- Scope-based access control
- API key validation

### Identity Management
- User profile management
- Organization management
- User-organization relationships
- Role management within organizations

### Audit and Compliance
- Security event logging
- Authentication attempt tracking
- Access logs
- Compliance reporting

## Architecture

The service is organized into clear modules:

```
com.codebridge.security
├── auth - Authentication functionality
│   ├── jwt - JWT token handling
│   ├── mfa - Multi-factor authentication
│   ├── model - Authentication models
│   ├── controller - Authentication endpoints
│   ├── service - Authentication business logic
│   └── repository - Data access
├── rbac - Role-based access control
│   ├── model - RBAC models
│   ├── controller - RBAC endpoints
│   ├── service - RBAC business logic
│   └── repository - Data access
├── apikey - API key management
│   ├── model - API key models
│   ├── controller - API key endpoints
│   ├── service - API key business logic
│   ├── filter - API key authentication
│   ├── dto - API key data transfer objects
│   └── repository - Data access
├── identity - Identity management
│   ├── model - Identity models
│   ├── controller - Identity endpoints
│   ├── service - Identity business logic
│   ├── dto - Identity data transfer objects
│   └── repository - Data access
├── audit - Audit logging
├── config - Security configuration
└── SecurityApplication.java - Main application
```

## API Endpoints

### Authentication
- `POST /api/auth/register` - Register a new user
- `POST /api/auth/login` - Authenticate a user
- `POST /api/auth/refresh` - Refresh an access token
- `POST /api/auth/logout` - Invalidate a token
- `POST /api/auth/mfa/verify` - Verify MFA code

### RBAC
- `GET /api/rbac/roles` - List all roles
- `POST /api/rbac/roles` - Create a new role
- `GET /api/rbac/permissions` - List all permissions
- `POST /api/rbac/roles/{roleId}/permissions` - Assign permissions to a role
- `POST /api/rbac/users/{userId}/roles` - Assign roles to a user

### API Keys
- `GET /api/apikeys` - List API keys
- `POST /api/apikeys` - Create a new API key
- `DELETE /api/apikeys/{keyId}` - Revoke an API key
- `PUT /api/apikeys/{keyId}/scopes` - Update API key scopes
- `PUT /api/apikeys/{keyId}/rate-limits` - Update API key rate limits
- `PUT /api/apikeys/{keyId}/ip-restrictions` - Update API key IP restrictions

### Identity Management
- `GET /api/identity/users` - List users
- `GET /api/identity/users/{userId}` - Get user details
- `PUT /api/identity/users/{userId}` - Update user details
- `GET /api/identity/organizations` - List organizations
- `POST /api/identity/organizations` - Create a new organization
- `GET /api/identity/organizations/{orgId}` - Get organization details
- `PUT /api/identity/organizations/{orgId}` - Update organization details

## Getting Started

### Prerequisites
- Java 21
- Maven 3.6+
- Redis (for production, optional for standalone mode)

### Configuration
The service can be configured through application.yml or environment variables.

#### Standalone Profile
The application includes a standalone profile (`application-standalone.yml`) that:
- Uses H2 in-memory database
- Configures Redis connection
- Allows circular references for complex dependency injection
- Sets up development-friendly logging

### Building
```bash
mvn clean package -DskipTests
```

### Running

#### Standalone Mode (Development)
```bash
java -jar target/codebridge-security-0.1.0-SNAPSHOT.jar --spring.profiles.active=standalone
```

#### External Database Mode (Production)
```bash
java -jar target/codebridge-security-0.1.0-SNAPSHOT.jar --spring.profiles.active=external
```

This mode connects to external PostgreSQL and Redis instances. Configure the following:
- PostgreSQL: 10.212.183.94:5432 (database: codebridge_security)
- Redis: 10.212.183.94:6379

The application will start on port 8080 with context path `/security`.

#### Using Docker
```bash
docker compose up --build
```

This will start:
- Redis server on port 6379
- CodeBridge Security application on port 8080

### Testing
```bash
# Health check
curl http://localhost:8080/security/actuator/health

# Test authentication endpoint
curl -X POST http://localhost:8080/security/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"test"}'
```

### Resolved Issues
1. **Circular Dependencies**: Separated PasswordEncoder configuration into its own class
2. **Duplicate Controllers**: Changed identity controller mapping from `/api/auth` to `/api/identity`
3. **Entity Conflicts**: Removed duplicate Role entity from identity package
4. **Build Configuration**: Updated to use Java 21 and Spring Boot 3.2.0

## Integration with Other Services
This service integrates with other CodeBridge services:
- Provides authentication for all services
- Manages API keys for external API access
- Handles user and organization management
