# CodeBridge Microservices Platform

CodeBridge is a comprehensive microservices platform for managing development environments, API testing, and team collaboration.

## Architecture

The platform consists of the following microservices:

### Core Services

1. **Auth Gateway Service**
   - API Gateway with routing and load balancing
   - Authentication and authorization with JWT
   - Role-based access control
   - Team-based resource isolation

2. **Docker Service**
   - Container management (create, start, stop, remove)
   - Resource limits (CPU, memory)
   - Team-based access control
   - Container monitoring

3. **API Testing Service**
   - HTTP request testing
   - Response validation
   - Test scripting
   - Test results tracking

### Security Features

- JWT-based authentication
- Role-based access control
- Team resource isolation
- Service-to-service authentication
- Audit logging

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+
- Docker
- PostgreSQL (optional, can use H2 for development)

### Building the Project

```bash
mvn clean install
```

### Running the Services

1. Start the Auth Gateway:

```bash
cd codebridge-auth-gateway
mvn spring-boot:run
```

2. Start the Docker Service:

```bash
cd codebridge-docker-service
mvn spring-boot:run
```

3. Start the API Testing Service:

```bash
cd codebridge-api-test-service
mvn spring-boot:run
```

## API Documentation

### Auth Gateway API

- `POST /api/login` - Authenticate user
- `POST /api/register` - Register new user
- `POST /api/refresh-token` - Refresh JWT token

### Docker Service API

- `POST /api/containers` - Create container
- `GET /api/containers` - List containers
- `GET /api/containers/{id}` - Get container
- `PUT /api/containers/{id}/start` - Start container
- `PUT /api/containers/{id}/stop` - Stop container
- `DELETE /api/containers/{id}` - Remove container

### API Testing Service

- `POST /api/tests` - Create API test
- `GET /api/tests` - List API tests
- `GET /api/tests/{id}` - Get API test
- `PUT /api/tests/{id}` - Update API test
- `DELETE /api/tests/{id}` - Delete API test
- `POST /api/tests/{id}/execute` - Execute API test
- `GET /api/tests/{id}/results` - Get test results

## Security

The platform implements multiple layers of security:

1. **Authentication** - JWT-based authentication for all services
2. **Authorization** - Role-based access control for API endpoints
3. **Resource Isolation** - Team-based resource isolation
4. **Service Security** - Service-to-service authentication
5. **Audit Logging** - Comprehensive audit logging for all operations

## Contributing

Please read [CONTRIBUTING.md](CONTRIBUTING.md) for details on our code of conduct and the process for submitting pull requests.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

