# CodeBridge

CodeBridge is a comprehensive microservices platform for managing and integrating various development tools and services.

## Project Structure

The CodeBridge platform consists of the following microservices:

- **codebridge-user-service**: User management, authentication, and authorization
- **codebridge-docker-service**: Docker registry and container management
- **codebridge-server-service**: Server management with time-based access expiry
- **codebridge-api-testing-service**: API testing, scheduling, and reporting
- **codebridge-gateway**: API Gateway for routing and security

## Features

### Docker Integration Features

- **Registry Management**:
  - Registry browser with authentication and SSL settings
  - Image listing and tag management
  - Connection testing

- **Container Management**:
  - Context management for multiple Docker environments
  - Container lifecycle operations (start, stop, restart)
  - Resource monitoring
  - Volume and network management

- **Advanced Logging**:
  - Log streaming and management
  - Log filtering and search
  - Log retention policies

### Server Management Features

- **Server Access Control**:
  - Time-based access expiry
  - Role-based permissions
  - Access audit logging

- **Multi-Server UI**:
  - Unified dashboard for multiple servers
  - Server health monitoring
  - Resource utilization tracking

- **Advanced Log Streaming**:
  - Real-time log aggregation
  - Log analysis and alerting
  - Custom log parsers

### API Testing Features

- **Test Scheduling**:
  - Cron-based test scheduling
  - Parallel test execution
  - Conditional test execution

- **Reporting Dashboard**:
  - Test result visualization
  - Historical trend analysis
  - Performance metrics

- **CI/CD Integration**:
  - Webhook triggers
  - Pipeline integration
  - Automated deployment testing

### General Features

- **Enhanced Role-Based Access Controls**:
  - Fine-grained permission management
  - Dynamic role assignment
  - Permission inheritance

- **Webhook Management UI**:
  - Webhook creation and configuration
  - Event filtering
  - Delivery monitoring and retry

- **JetBrains Plugin Integration**:
  - Direct IDE integration
  - Code navigation
  - Remote debugging

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.8 or higher
- PostgreSQL 13 or higher
- Docker (for containerization)

### Building the Project

```bash
mvn clean install
```

### Running the Services

Each service can be run independently:

```bash
java -jar codebridge-user-service/target/codebridge-user-service-1.0.0-SNAPSHOT.jar
java -jar codebridge-docker-service/target/codebridge-docker-service-1.0.0-SNAPSHOT.jar
java -jar codebridge-server-service/target/codebridge-server-service-1.0.0-SNAPSHOT.jar
java -jar codebridge-api-testing-service/target/codebridge-api-testing-service-1.0.0-SNAPSHOT.jar
java -jar codebridge-gateway/target/codebridge-gateway-1.0.0-SNAPSHOT.jar
```

### Docker Deployment

```bash
docker-compose up -d
```

## API Documentation

Each service provides Swagger/OpenAPI documentation at:

- User Service: http://localhost:8081/swagger-ui.html
- Docker Service: http://localhost:8084/swagger-ui.html
- Server Service: http://localhost:8085/swagger-ui.html
- API Testing Service: http://localhost:8086/swagger-ui.html
- Gateway: http://localhost:8080/swagger-ui.html

## Contributing

Please read [CONTRIBUTING.md](CONTRIBUTING.md) for details on our code of conduct and the process for submitting pull requests.

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

