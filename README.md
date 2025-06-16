# CodeBridge - GitLab and Docker Services

This project provides microservices for integrating with GitLab and Docker, allowing for seamless management of CI/CD pipelines and containerized applications.

## Services

### GitLab Service

The GitLab Service provides a RESTful API for interacting with GitLab, focusing on:

- Authentication with GitLab using personal access tokens
- Project management
- Pipeline management
- Job management and log retrieval

### Docker Service

The Docker Service provides a RESTful API for interacting with Docker, focusing on:

- Authentication with Docker Registry
- Container management (create, start, stop, remove)
- Container log retrieval with filtering options
- Image management (pull, remove, build)

## Technology Stack

- Java 17
- Spring Boot 3.2.0
- Spring Cloud 2023.0.0
- Spring Security with JWT authentication
- Docker Java Client 3.3.3
- Maven

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- Docker (for Docker Service)
- GitLab account (for GitLab Service)

### Building the Services

```bash
# Build GitLab Service
cd gitlab-service
mvn clean package

# Build Docker Service
cd ../docker-service
mvn clean package
```

### Running the Services

```bash
# Run GitLab Service
cd gitlab-service
java -jar target/gitlab-service-0.0.1-SNAPSHOT.jar

# Run Docker Service
cd ../docker-service
java -jar target/docker-service-0.0.1-SNAPSHOT.jar
```

## API Documentation

### GitLab Service API

- **Base URL**: `/api/gitlab`

#### Authentication

- `POST /auth/login`: Authenticate with GitLab

#### Projects

- `GET /projects`: Get all accessible projects
- `GET /projects/{projectId}`: Get a specific project

#### Pipelines

- `GET /projects/{projectId}/pipelines`: Get all pipelines for a project
- `GET /projects/{projectId}/pipelines/{pipelineId}`: Get a specific pipeline
- `GET /projects/{projectId}/pipelines/{pipelineId}/jobs`: Get jobs for a pipeline

#### Jobs

- `GET /projects/{projectId}/jobs`: Get all jobs for a project
- `GET /projects/{projectId}/jobs/{jobId}`: Get a specific job
- `GET /projects/{projectId}/jobs/{jobId}/logs`: Get logs for a job

### Docker Service API

- **Base URL**: `/api/docker`

#### Authentication

- `POST /auth/login`: Authenticate with Docker Registry

#### Containers

- `GET /containers`: Get all containers
- `GET /containers/{containerId}`: Get a specific container
- `POST /containers`: Create a new container
- `POST /containers/{containerId}/start`: Start a container
- `POST /containers/{containerId}/stop`: Stop a container
- `DELETE /containers/{containerId}`: Remove a container
- `GET /containers/{containerId}/logs`: Get logs for a container

#### Images

- `GET /images`: Get all images
- `GET /images/{imageId}`: Get a specific image
- `POST /images/pull`: Pull an image
- `DELETE /images/{imageId}`: Remove an image
- `POST /images/build`: Build an image

## Security

Both services implement JWT-based authentication for secure API access. The authentication flow is as follows:

1. Client authenticates with the service using credentials or tokens
2. Service validates credentials and returns a JWT token
3. Client includes the JWT token in the Authorization header for subsequent requests
4. Service validates the token for each request

## Configuration

Configuration for both services is managed through `application.yml` files:

- GitLab Service: `gitlab-service/src/main/resources/application.yml`
- Docker Service: `docker-service/src/main/resources/application.yml`

Key configuration properties:

### GitLab Service

```yaml
gitlab:
  api:
    base-url: https://gitlab.com/api/v4
    connect-timeout: 5000
    read-timeout: 30000
    write-timeout: 10000
  auth:
    token-expiration: 86400000
```

### Docker Service

```yaml
docker:
  api:
    host: unix:///var/run/docker.sock
    connect-timeout: 5000
    read-timeout: 30000
    write-timeout: 10000
  auth:
    token-expiration: 86400000
```

## License

This project is licensed under the MIT License - see the LICENSE file for details.

