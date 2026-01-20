# CodeBridge Documentation Service

The CodeBridge Documentation Service provides comprehensive API documentation and developer experience capabilities for the CodeBridge platform. It automatically generates, versions, and publishes API documentation, as well as provides interactive documentation, code samples, and search functionality.

## Features

- **API Documentation Generation**: Automatically generates documentation from OpenAPI specifications
- **Documentation Versioning**: Manages multiple versions of API documentation
- **Interactive Documentation**: Provides interactive API exploration and testing
- **Code Sample Generation**: Generates code samples in multiple programming languages
- **Client Library Generation**: Creates client libraries for easy API integration
- **Search Functionality**: Enables searching across all API documentation
- **Publishing Capabilities**: Publishes documentation to multiple targets (file system, S3, Git, FTP)

## Architecture

The Documentation Service consists of several components:

- **Documentation Service**: Central service for managing API documentation
- **OpenAPI Service**: Handles OpenAPI specification parsing and validation
- **Versioning Service**: Manages API versions and their lifecycle
- **Storage Service**: Stores documentation files in various formats
- **Publishing Service**: Publishes documentation to different targets
- **Code Generation Service**: Generates code samples and client libraries
- **Interactive Documentation Service**: Provides interactive API exploration and search

## API Endpoints

The service exposes the following API endpoints:

- `/api/docs/documentation`: Endpoints for managing API documentation
- `/api/docs/services`: Endpoints for managing services
- `/api/docs/versions`: Endpoints for managing API versions
- `/api/docs/code-samples`: Endpoints for managing code samples
- `/api/docs/examples`: Endpoints for managing API examples
- `/api/docs/search`: Endpoints for searching API documentation
- `/api/docs/public`: Public endpoints for accessing documentation

## Configuration

The service can be configured using the following properties in `application.yml`:

```yaml
documentation:
  storage:
    base-path: ${user.home}/codebridge/documentation
  openapi:
    scan-enabled: true
    scan-packages: com.codebridge
    scan-interval: 3600000  # 1 hour in milliseconds
  versioning:
    enabled: true
    strategy: semantic  # semantic, date, or custom
    default-version: latest
  publishing:
    auto-publish: true
    publish-interval: 86400000  # 24 hours in milliseconds
  code-generation:
    enabled: true
    languages:
      - java
      - python
      - javascript
      - typescript
      - csharp
      - go
    templates-path: ${user.home}/codebridge/templates
  interactive:
    enabled: true
    examples-enabled: true
    search-enabled: true
```

## Getting Started

### Prerequisites

- Java 17 or higher
- PostgreSQL database
- Maven

### Running the Service

1. Clone the repository
2. Configure the database connection in `application.yml`
3. Build the service: `mvn clean package`
4. Run the service: `java -jar target/codebridge-documentation-service-1.0.0.jar`

### Docker

You can also run the service using Docker:

```bash
docker build -t codebridge/documentation-service .
docker run -p 8087:8087 codebridge/documentation-service
```

Or using Docker Compose:

```bash
docker-compose up -d documentation-service
```

## API Documentation

The service provides its own API documentation using Swagger UI, which can be accessed at:

```
http://localhost:8087/api/docs/swagger-ui.html
```

## License

This project is licensed under the Apache License 2.0 - see the LICENSE file for details.

