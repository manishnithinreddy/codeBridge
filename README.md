# CodeBridge

CodeBridge is a scalable, multi-language platform designed to provide a comprehensive set of services for software development and integration.

## Architecture Overview

CodeBridge follows a microservices architecture with services implemented in different programming languages based on their specific requirements and strengths.

### Directory Structure

```
codebridge/
├── teams-service/           # Teams and collaboration service
├── api-test-service/        # API testing and validation service
├── gitlab-service/          # GitLab integration service
├── docker-service/          # Docker management service
├── server-service/          # Server management service
├── session-service/         # Authentication and session management
│   ├── java-implementation/ # Java implementation
│   └── go-implementation/   # Go implementation
├── db-service/              # Database management service
│   ├── java-implementation/ # Java implementation
│   └── go-implementation/   # Go implementation
└── ai-service/              # AI and machine learning service
    └── python-implementation/ # Python implementation
```

### Core Services

#### Session Service (Go Implementation)

The Session Service handles user authentication, session management, and token handling. It provides:

- User registration and authentication
- JWT-based token generation and validation
- Session management
- Refresh token handling
- Redis-backed storage for scalability
- Secure password hashing with bcrypt

#### DB Service (Go Implementation)

The DB Service manages database connections, query execution, and schema information retrieval. It supports:

- Multiple database types (MySQL, PostgreSQL, SQLite)
- Connection pooling and management
- Query execution with parameter binding
- Batch query execution
- Transaction support
- Schema information retrieval

#### AI Service (Python Implementation)

The AI Service provides artificial intelligence capabilities such as text completion and embeddings. It features:

- Text completion using various models
- Text embedding generation
- Model information retrieval
- Support for multiple model providers (currently OpenAI)

### Communication Between Services

Services communicate with each other via RESTful APIs. Authentication between services is handled using JWT tokens validated by the Session Service.

## Getting Started

### Prerequisites

- Go 1.20 or higher (for Go services)
- Python 3.10 or higher (for Python services)
- Redis 6.0 or higher (for Session Service)
- MySQL, PostgreSQL, or SQLite (for DB Service)
- OpenAI API key (for AI Service)

### Building and Running

Each service can be built and run independently. See the README.md file in each service directory for specific instructions.

#### Using Docker

Each service includes a Dockerfile for containerized deployment:

```bash
# Build and run Session Service
cd session-service/go-implementation
docker build -t codebridge/session-service-go .
docker run -p 8080:8080 codebridge/session-service-go

# Build and run DB Service
cd db-service/go-implementation
docker build -t codebridge/db-service-go .
docker run -p 8081:8081 codebridge/db-service-go

# Build and run AI Service
cd ai-service/python-implementation
docker build -t codebridge/ai-service-python .
docker run -p 8082:8082 -e OPENAI_API_KEY=your-api-key codebridge/ai-service-python
```

## Scalability Considerations

CodeBridge is designed for horizontal scalability:

- **Stateless Design**: Services maintain no local state
- **Distributed Storage**: Redis for session data, databases for persistent storage
- **Connection Pooling**: Efficient resource management
- **Graceful Shutdown**: Proper handling of shutdown signals
- **Health Checks**: Monitoring endpoints for load balancers

## Security Features

- **JWT Authentication**: Token-based authentication across services
- **Password Hashing**: Secure password storage with bcrypt
- **Token Validation**: Comprehensive token validation
- **Parameter Binding**: Protection against SQL injection
- **CORS Protection**: Configurable CORS headers

## Future Enhancements

- Implement additional services (Teams, API Test, GitLab, Docker, Server)
- Add Java implementations for Session and DB services
- Implement service discovery and registration
- Add metrics collection and monitoring
- Implement CI/CD pipelines
- Add comprehensive documentation

