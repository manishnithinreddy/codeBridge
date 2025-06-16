# CodeBridge GitLab Service

This service provides a RESTful API for interacting with GitLab, allowing users to manage projects, pipelines, and jobs.

## Features

- Authentication with GitLab personal access tokens
- Project management (list, get, create, archive/unarchive)
- Pipeline management (list, get, create, cancel, retry)
- Job management (list, get, logs)
- JWT-based security
- Comprehensive error handling
- Swagger/OpenAPI documentation

## Technology Stack

- Java 21
- Spring Boot 3.2.0
- Spring Cloud 2023.0.0
- JWT for security
- RESTful API design
- Lombok for boilerplate reduction
- SLF4J for logging

## Getting Started

### Prerequisites

- Java 21 or higher
- Maven 3.8.0 or higher
- Docker (optional, for containerization)

### Building the Service

```bash
mvn clean package
```

### Running the Service

```bash
java -jar target/codebridge-gitlab-service-0.0.1-SNAPSHOT.jar
```

### Running with Docker

```bash
docker build -t codebridge/gitlab-service .
docker run -p 8081:8081 codebridge/gitlab-service
```

## API Documentation

The API documentation is available at `/api/gitlab/swagger-ui.html` when the service is running.

### Authentication

To authenticate with the GitLab service, you need to obtain a JWT token by sending a POST request to `/api/gitlab/auth/login` with your GitLab personal access token:

```json
{
  "token": "your_gitlab_personal_access_token"
}
```

The response will contain a JWT token that you can use for subsequent requests:

```json
{
  "token": "jwt_token",
  "tokenType": "Bearer",
  "expiresIn": 86400
}
```

### Projects

#### List Projects

```
GET /api/gitlab/projects
```

Query parameters:
- `owned` (boolean): Only return projects owned by the authenticated user
- `search` (string): Search projects by name

#### Get Project

```
GET /api/gitlab/projects/{projectId}
```

#### Create Project

```
POST /api/gitlab/projects
```

Request body:
```json
{
  "name": "project_name",
  "description": "project_description",
  "visibility": "private"
}
```

#### Archive Project

```
POST /api/gitlab/projects/{projectId}/archive
```

#### Unarchive Project

```
POST /api/gitlab/projects/{projectId}/unarchive
```

### Pipelines

#### List Pipelines

```
GET /api/gitlab/projects/{projectId}/pipelines
```

Query parameters:
- `status` (string): Filter pipelines by status (running, pending, success, failed, canceled)
- `ref` (string): Filter pipelines by ref (branch or tag)

#### Get Pipeline

```
GET /api/gitlab/projects/{projectId}/pipelines/{pipelineId}
```

#### Create Pipeline

```
POST /api/gitlab/projects/{projectId}/pipelines
```

Request body:
```json
{
  "ref": "branch_name"
}
```

#### Cancel Pipeline

```
POST /api/gitlab/projects/{projectId}/pipelines/{pipelineId}/cancel
```

#### Retry Pipeline

```
POST /api/gitlab/projects/{projectId}/pipelines/{pipelineId}/retry
```

### Jobs

#### List Jobs

```
GET /api/gitlab/projects/{projectId}/jobs
```

Query parameters:
- `scope` (string): Filter jobs by scope (created, pending, running, failed, success, canceled, skipped, manual)

#### Get Job

```
GET /api/gitlab/projects/{projectId}/jobs/{jobId}
```

#### Get Job Logs

```
GET /api/gitlab/projects/{projectId}/jobs/{jobId}/logs
```

## Configuration

The service can be configured using the `application.yml` file:

```yaml
server:
  port: 8081
  servlet:
    context-path: /api/gitlab

spring:
  application:
    name: codebridge-gitlab-service
  profiles:
    active: dev

gitlab:
  api:
    base-url: https://gitlab.com/api/v4
    connect-timeout: 5000
    read-timeout: 30000
    write-timeout: 10000
  auth:
    token-expiration: 86400000

logging:
  level:
    root: INFO
    com.codebridge: DEBUG
    org.springframework.web: INFO
```

## Testing

The service includes unit tests and integration tests. To run the tests:

```bash
mvn test
```

## CI/CD

The service includes a GitHub Actions workflow for CI/CD. The workflow builds the service, runs tests, and publishes a Docker image to DockerHub.

## License

This project is licensed under the MIT License - see the LICENSE file for details.

