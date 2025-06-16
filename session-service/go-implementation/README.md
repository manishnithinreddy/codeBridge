# Session Service (Go Implementation)

This is the Go implementation of the CodeBridge Session Service, responsible for user authentication, session management, and token handling.

## Features

- User registration and authentication
- JWT-based token generation and validation
- Session management
- Refresh token handling
- Redis-backed storage for scalability
- Secure password hashing with bcrypt

## Architecture

The service follows a clean architecture approach:

- **API Layer**: HTTP handlers and middleware
- **Service Layer**: Business logic
- **Repository Layer**: Data storage and retrieval
- **Model Layer**: Domain models and entities

## Prerequisites

- Go 1.20 or higher
- Redis 6.0 or higher

## Configuration

Configuration is loaded from YAML files and environment variables:

```yaml
server:
  port: 8080
  readTimeout: 10s
  writeTimeout: 10s
  idleTimeout: 60s

redis:
  host: localhost
  port: 6379
  password: ""
  db: 0
  poolSize: 10

jwt:
  secret: "your-jwt-secret"
  refreshSecret: "your-refresh-secret"
  expirationHours: 24
  refreshExpHours: 168
  issuer: "codebridge-session-service"
  accessTokenName: "access_token"
  refreshTokenName: "refresh_token"
```

Environment variables can override configuration values with the `SESSION_` prefix:

```bash
SESSION_SERVER_PORT=8081
SESSION_REDIS_HOST=redis.example.com
SESSION_JWT_SECRET=your-secret-key
```

## Building and Running

### Local Development

```bash
# Build the service
go build -o session-service ./cmd/server

# Run the service
./session-service
```

### Docker

```bash
# Build the Docker image
docker build -t codebridge/session-service-go .

# Run the container
docker run -p 8080:8080 codebridge/session-service-go
```

## API Endpoints

### Public Endpoints

- `POST /api/register`: Register a new user
- `POST /api/login`: Authenticate a user and get tokens
- `POST /api/refresh`: Refresh an access token
- `POST /api/logout`: Invalidate a refresh token

### Protected Endpoints

- `GET /api/profile`: Get user profile
- `GET /api/sessions`: Get user sessions
- `POST /api/logout-all`: Logout from all devices

### Service Endpoints

- `POST /api/validate-token`: Validate a JWT token (for other services)
- `GET /health`: Health check endpoint

## Scalability Considerations

This implementation is designed for horizontal scalability:

- **Stateless Design**: No local state is maintained
- **Redis Backend**: Distributed session storage
- **Connection Pooling**: Efficient Redis connection management
- **Graceful Shutdown**: Proper handling of shutdown signals
- **Health Checks**: Monitoring endpoint for load balancers

## Security Features

- **JWT Tokens**: Short-lived access tokens with refresh capability
- **Password Hashing**: Secure password storage with bcrypt
- **Token Validation**: Comprehensive token validation
- **Session Management**: Ability to invalidate sessions
- **CORS Protection**: Configurable CORS headers

## Monitoring and Observability

- **Structured Logging**: JSON-formatted logs for easy parsing
- **Request Logging**: Detailed request logs
- **Health Checks**: Endpoint for monitoring service health

