# CodeBridge API Test Service

A comprehensive API testing service for the CodeBridge platform. This service allows users to create, manage, and execute API tests, organize them into collections, and share them with other users.

## Features

- Create and manage API test projects
- Organize tests into collections
- Define test environments with variables
- Execute API tests with environment variable substitution
- Create and compare snapshots
- Share projects with other users
- Token-based authentication for API access

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6 or higher

### Running the Service

1. Clone the repository
2. Navigate to the project directory
3. Build the project:
   ```
   mvn clean package
   ```
4. Run the service:
   ```
   java -jar target/codebridge-api-test-service-1.0.0-SNAPSHOT.jar
   ```

The service will start on port 8084 by default.

### Testing the Service

Two test scripts are provided to verify the service functionality:

1. Shell script:
   ```
   ./test-api.sh
   ```

2. Python script:
   ```
   ./test_api.py
   ```

## API Endpoints

### Projects

- `GET /api/projects` - List all projects
- `POST /api/projects` - Create a new project
- `GET /api/projects/{projectId}` - Get project details
- `PUT /api/projects/{projectId}` - Update a project
- `DELETE /api/projects/{projectId}` - Delete a project

### Collections

- `GET /api/projects/{projectId}/collections` - List collections in a project
- `POST /api/projects/{projectId}/collections` - Create a new collection
- `GET /api/collections/{collectionId}` - Get collection details
- `PUT /api/collections/{collectionId}` - Update a collection
- `DELETE /api/collections/{collectionId}` - Delete a collection

### Tests

- `GET /api/collections/{collectionId}/tests` - List tests in a collection
- `POST /api/collections/{collectionId}/tests` - Create a new test
- `GET /api/tests/{testId}` - Get test details
- `PUT /api/tests/{testId}` - Update a test
- `DELETE /api/tests/{testId}` - Delete a test
- `POST /api/tests/{testId}/run` - Run a test
- `GET /api/tests/{testId}/results` - Get test results

### Environments

- `GET /api/projects/{projectId}/environments` - List environments in a project
- `POST /api/projects/{projectId}/environments` - Create a new environment
- `GET /api/environments/{environmentId}` - Get environment details
- `PUT /api/environments/{environmentId}` - Update an environment
- `DELETE /api/environments/{environmentId}` - Delete an environment

### Snapshots

- `POST /api/tests/{testId}/snapshots` - Create a snapshot
- `GET /api/tests/{testId}/snapshots` - List snapshots for a test
- `GET /api/snapshots/{snapshotId}` - Get snapshot details
- `DELETE /api/snapshots/{snapshotId}` - Delete a snapshot
- `POST /api/tests/{testId}/compare` - Compare test results with a snapshot

### Sharing

- `POST /api/projects/{projectId}/shares` - Share a project with a user
- `GET /api/projects/{projectId}/shares` - List users with access to a project
- `DELETE /api/projects/{projectId}/shares/users/{userId}` - Revoke access

### Tokens

- `POST /api/projects/{projectId}/tokens` - Create an API token
- `GET /api/projects/{projectId}/tokens` - List tokens for a project
- `DELETE /api/tokens/{tokenId}` - Delete a token

## Configuration

The service can be configured using the `application.yml` file:

```yaml
spring:
  application:
    name: codebridge-api-test-service
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password: password
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    hibernate:
      ddl-auto: update
  h2:
    console:
      enabled: true
      path: /h2-console

server:
  port: 8084

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
```

## Security

The service uses Spring Security with JWT authentication. For development purposes, all endpoints are currently accessible without authentication.

## License

This project is licensed under the MIT License.

