# CodeBridge GitLab and Docker Services

This repository contains two microservices for the CodeBridge platform:

1. **GitLab Service**: A RESTful API for interacting with GitLab, allowing users to manage projects, pipelines, and jobs.
2. **Docker Service**: A RESTful API for interacting with Docker, allowing users to manage containers, images, and registries.

## Technology Stack

- Java 21
- Spring Boot 3.2.0
- Spring Cloud 2023.0.0
- Docker Java Client 3.3.3
- JWT for security
- RESTful API design
- Lombok for boilerplate reduction
- SLF4J for logging

## Services

### GitLab Service

The GitLab Service provides the following features:

- Authentication with GitLab personal access tokens
- Project management (list, get, create, archive/unarchive)
- Pipeline management (list, get, create, cancel, retry)
- Job management (list, get, logs)
- JWT-based security
- Comprehensive error handling
- Swagger/OpenAPI documentation

For more details, see the [GitLab Service README](codebridge-gitlab-service/README.md).

### Docker Service

The Docker Service provides the following features:

- Authentication with Docker Registry
- Container management (list, get, create, start, stop, restart, logs, stats)
- Image management (list, get, pull, push, build, tag, remove)
- JWT-based security
- Comprehensive error handling
- Swagger/OpenAPI documentation

For more details, see the [Docker Service README](codebridge-docker-service/README.md).

## Getting Started

### Prerequisites

- Java 21 or higher
- Maven 3.8.0 or higher
- Docker (required for Docker Service functionality)

### Running with Docker Compose

The easiest way to run both services is using Docker Compose:

```bash
docker-compose up -d
```

This will build and start both services:

- GitLab Service: http://localhost:8081/api/gitlab
- Docker Service: http://localhost:8082/api/docker

### Building and Running Manually

To build and run the services manually:

#### GitLab Service

```bash
cd codebridge-gitlab-service
mvn clean package
java -jar target/codebridge-gitlab-service-0.0.1-SNAPSHOT.jar
```

#### Docker Service

```bash
cd codebridge-docker-service
mvn clean package
java -jar target/codebridge-docker-service-0.0.1-SNAPSHOT.jar
```

## API Documentation

Each service provides Swagger/OpenAPI documentation:

- GitLab Service: http://localhost:8081/api/gitlab/swagger-ui.html
- Docker Service: http://localhost:8082/api/docker/swagger-ui.html

## Testing

Each service includes unit tests and integration tests. To run the tests:

```bash
# GitLab Service
cd codebridge-gitlab-service
mvn test

# Docker Service
cd codebridge-docker-service
mvn test
```

## CI/CD

Each service includes a GitHub Actions workflow for CI/CD. The workflows build the services, run tests, and publish Docker images to DockerHub.

## License

This project is licensed under the MIT License - see the LICENSE file for details.

