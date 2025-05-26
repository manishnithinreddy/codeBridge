# CodeBridge Platform

CodeBridge is a comprehensive platform for code management, deployment, and testing. It provides a set of microservices that work together to streamline the development workflow.

## Microservices Architecture

The platform consists of the following microservices:

1. **Git Service** - Manages Git repositories, providers, webhooks, and credentials
2. **Docker Service** - Handles Docker container management and orchestration
3. **Server Access Service** - Provides secure access to remote servers via SSH
4. **API Testing Service** - Automates API testing and monitoring

## Technology Stack

- Java 21
- Spring Boot 3.2.0
- Spring Cloud
- PostgreSQL
- Flyway
- Docker & Docker Compose
- JGit, Docker Java, JSch, REST Assured
- OAuth2 / JWT Authentication
- Service Discovery with Eureka
- API Documentation with SpringDoc OpenAPI

## Getting Started

### Prerequisites

- Java 21 or higher
- Maven
- Docker and Docker Compose
- PostgreSQL

### Building the Project

To build all microservices:

```bash
mvn clean install
```

### Running the Services

Each service can be run independently:

```bash
cd codebridge-git-service
./mvnw spring-boot:run
```

Or using Docker Compose:

```bash
cd codebridge-git-service
docker-compose up
```

## Service Endpoints

- Git Service: http://localhost:8081/api/git
- Docker Service: http://localhost:8082/api/docker
- Server Access Service: http://localhost:8083/api/server
- API Testing Service: http://localhost:8084/api/testing

## API Documentation

Each service provides its own Swagger UI for API documentation:

- Git Service: http://localhost:8081/api/git/swagger-ui/index.html
- Docker Service: http://localhost:8082/api/docker/swagger-ui/index.html
- Server Access Service: http://localhost:8083/api/server/swagger-ui/index.html
- API Testing Service: http://localhost:8084/api/testing/swagger-ui/index.html

## Development

### Project Structure

```
codebridge/
├── codebridge-git-service/
├── codebridge-docker-service/
├── codebridge-server-service/
├── codebridge-api-testing-service/
└── pom.xml
```

### Adding a New Service

1. Create a new directory for your service
2. Add a pom.xml file with the necessary dependencies
3. Implement the service using Spring Boot
4. Add the new module to the parent pom.xml

## License

This project is licensed under the Apache License 2.0 - see the LICENSE file for details.

