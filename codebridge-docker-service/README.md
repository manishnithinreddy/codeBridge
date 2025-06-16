# CodeBridge Docker Service

This service provides a RESTful API for interacting with Docker, allowing users to manage containers, images, and registries.

## Features

- Authentication with Docker Registry
- Container management (list, get, create, start, stop, restart, logs, stats)
- Image management (list, get, pull, push, build, tag, remove)
- JWT-based security
- Comprehensive error handling
- Swagger/OpenAPI documentation

## Technology Stack

- Java 21
- Spring Boot 3.2.0
- Spring Cloud 2023.0.0
- Docker Java Client 3.3.3
- JWT for security
- RESTful API design
- Lombok for boilerplate reduction
- SLF4J for logging

## Getting Started

### Prerequisites

- Java 21 or higher
- Maven 3.8.0 or higher
- Docker (required for functionality)

### Building the Service

```bash
mvn clean package
```

### Running the Service

```bash
java -jar target/codebridge-docker-service-0.0.1-SNAPSHOT.jar
```

### Running with Docker

```bash
docker build -t codebridge/docker-service .
docker run -v /var/run/docker.sock:/var/run/docker.sock -p 8082:8082 codebridge/docker-service
```

Note: The Docker socket must be mounted to allow the service to interact with the Docker daemon.

## API Documentation

The API documentation is available at `/api/docker/swagger-ui.html` when the service is running.

### Authentication

To authenticate with the Docker service, you need to obtain a JWT token by sending a POST request to `/api/docker/auth/login` with your Docker Registry credentials:

```json
{
  "username": "your_username",
  "password": "your_password",
  "registry": "registry.example.com"
}
```

The response will contain a JWT token that you can use for subsequent requests:

```json
{
  "token": "jwt_token",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "username": "your_username"
}
```

### Containers

#### List Containers

```
GET /api/docker/containers
```

Query parameters:
- `showAll` (boolean): Whether to show all containers (including stopped ones)

#### Get Container

```
GET /api/docker/containers/{containerIdOrName}
```

#### Create Container

```
POST /api/docker/containers
```

Query parameters:
- `image` (string): Image to use for the container
- `name` (string): Name for the container
- `env` (map): Environment variables
- `ports` (map): Port mappings
- `volumes` (map): Volume mappings
- `cmd` (array): Command to run

#### Start Container

```
POST /api/docker/containers/{containerIdOrName}/start
```

#### Stop Container

```
POST /api/docker/containers/{containerIdOrName}/stop
```

Query parameters:
- `timeout` (integer): Timeout in seconds before killing the container

#### Restart Container

```
POST /api/docker/containers/{containerIdOrName}/restart
```

Query parameters:
- `timeout` (integer): Timeout in seconds before killing the container

#### Get Container Logs

```
GET /api/docker/containers/{containerIdOrName}/logs
```

Query parameters:
- `tail` (integer): Number of lines to show from the end of the logs
- `timestamps` (boolean): Whether to show timestamps

#### Get Container Stats

```
GET /api/docker/containers/{containerIdOrName}/stats
```

#### Remove Container

```
DELETE /api/docker/containers/{containerIdOrName}
```

Query parameters:
- `removeVolumes` (boolean): Whether to remove volumes
- `force` (boolean): Whether to force removal

### Images

#### List Images

```
GET /api/docker/images
```

Query parameters:
- `showAll` (boolean): Whether to show all images (including intermediate images)

#### Get Image

```
GET /api/docker/images/{imageIdOrName}
```

#### Pull Image

```
POST /api/docker/images/pull
```

Query parameters:
- `imageName` (string): Name of the image to pull
- `tag` (string): Tag of the image to pull
- `registry` (string): Registry to pull from
- `username` (string): Username for registry authentication
- `password` (string): Password for registry authentication

#### Push Image

```
POST /api/docker/images/push
```

Query parameters:
- `imageName` (string): Name of the image to push
- `tag` (string): Tag of the image to push
- `registry` (string): Registry to push to
- `username` (string): Username for registry authentication
- `password` (string): Password for registry authentication

#### Get Image History

```
GET /api/docker/images/{imageIdOrName}/history
```

#### Search Images

```
GET /api/docker/images/search
```

Query parameters:
- `term` (string): Search term
- `limit` (integer): Maximum number of results

#### Remove Image

```
DELETE /api/docker/images/{imageIdOrName}
```

Query parameters:
- `force` (boolean): Whether to force removal
- `noPrune` (boolean): Whether to prevent pruning

#### Tag Image

```
POST /api/docker/images/{imageIdOrName}/tag
```

Query parameters:
- `repositoryName` (string): Repository name for the new tag
- `tag` (string): New tag

## Configuration

The service can be configured using the `application.yml` file:

```yaml
server:
  port: 8082
  servlet:
    context-path: /api/docker

spring:
  application:
    name: codebridge-docker-service
  profiles:
    active: dev

docker:
  api:
    host: unix:///var/run/docker.sock
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

## Security Considerations

- The service requires access to the Docker socket, which can be a security risk if not properly secured.
- The service should be run with appropriate permissions and access controls.
- The JWT tokens should be kept secure and not shared.

## License

This project is licensed under the MIT License - see the LICENSE file for details.

