# CodeBridge Git Service

This microservice is responsible for managing Git repositories, providers, webhooks, and credentials in the CodeBridge platform.

## Features

- Support for multiple Git providers (GitHub, GitLab, Bitbucket)
- Repository management
- Webhook configuration and handling
- Credential management (PAT, OAuth, SSH)
- Integration with other CodeBridge services

## Technology Stack

- Java 21
- Spring Boot 3.2.0
- Spring Cloud
- JGit
- PostgreSQL
- Flyway
- MapStruct
- SpringDoc OpenAPI

## Getting Started

### Prerequisites

- Java 21 or higher
- Maven
- Docker and Docker Compose (for local development)

### Running Locally

1. Clone the repository
2. Start the PostgreSQL database:

```bash
docker-compose up -d postgres
```

3. Run the application:

```bash
./mvnw spring-boot:run
```

Or using Docker Compose:

```bash
docker-compose up
```

### API Documentation

Once the application is running, you can access the API documentation at:

```
http://localhost:8081/api/git/swagger-ui/index.html
```

## Configuration

The application can be configured using the `application.yml` file or environment variables.

### Database Configuration

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/codebridge_git
    username: postgres
    password: postgres
```

### Git Provider Configuration

```yaml
git:
  workspace:
    base-path: /tmp/codebridge/git-workspace
  providers:
    github:
      api-url: https://api.github.com
    gitlab:
      api-url: https://gitlab.com/api/v4
    bitbucket:
      api-url: https://api.bitbucket.org/2.0
```

## Building

To build the application:

```bash
./mvnw clean package
```

To build the Docker image:

```bash
docker build -t codebridge/git-service .
```

## Testing

To run the tests:

```bash
./mvnw test
```

## License

This project is licensed under the Apache License 2.0 - see the LICENSE file for details.

