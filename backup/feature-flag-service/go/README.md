# CodeBridge Feature Flag Service (Go)

Go implementation of the Feature Flag Service for the CodeBridge microservices architecture.

## Overview

The Feature Flag Service provides dynamic feature flag management for the CodeBridge platform, enabling:

- Dynamic service implementation switching between different language implementations (Java, Go, Python)
- Feature toggling for gradual rollouts
- Context-based flag evaluation
- Real-time flag updates via Redis pub/sub

## Features

- **Flag Management**: Create, read, update, and delete feature flags
- **Context-Based Evaluation**: Evaluate flags based on user, session, and service context
- **Rule-Based Targeting**: Target specific users or services with rules
- **Real-time Updates**: Stream flag changes in real-time
- **Multiple Interfaces**: Both gRPC and REST APIs
- **High Performance**: Redis-backed storage with caching

## Architecture

The service is built with a clean architecture approach:

- **API Layer**: HTTP and gRPC interfaces
- **Service Layer**: Business logic and flag evaluation
- **Repository Layer**: Data storage and retrieval
- **Model Layer**: Domain models and entities

## Prerequisites

- Go 1.21 or higher
- Redis 6.0 or higher

## Configuration

Configuration is loaded from `config/application.yaml` and can be overridden with environment variables:

```yaml
server:
  port: 8090
  context-path: /feature-flag

redis:
  host: ${REDIS_HOST:localhost}
  port: ${REDIS_PORT:6379}
  password: ${REDIS_PASSWORD:}
  database: ${REDIS_DATABASE:0}
  timeout: 2000
  pool:
    max-active: 8
    max-idle: 8
    min-idle: 2
    max-wait: -1

grpc:
  server:
    port: 9090

feature-flag:
  cache:
    ttl-seconds: 60
  defaults:
    namespace: default
  pubsub:
    channel: feature-flag-updates
```

## Building and Running

### Local Development

```bash
# Install dependencies
go mod download

# Run the service
go run cmd/server/main.go
```

### Docker

```bash
# Build the Docker image
docker build -t codebridge/feature-flag-service:latest .

# Run the container
docker run -p 8090:8090 -p 9090:9090 -e REDIS_HOST=redis codebridge/feature-flag-service:latest
```

## API Usage

### REST API

#### Get a Flag

```bash
curl -X GET "http://localhost:8090/api/v1/flags/service-implementation?namespace=production"
```

#### Set a Flag

```bash
curl -X PUT "http://localhost:8090/api/v1/flags/service-implementation" \
  -H "Content-Type: application/json" \
  -d '{
    "namespace": "production",
    "valueType": "STRING",
    "value": "\"go\"",
    "description": "Controls which implementation of the service to use",
    "tags": {
      "category": "service-routing",
      "owner": "platform-team"
    }
  }'
```

#### Evaluate a Flag

```bash
curl -X POST "http://localhost:8090/api/v1/flags/service-implementation/evaluate" \
  -H "Content-Type: application/json" \
  -d '{
    "namespace": "production",
    "userId": "user-123",
    "sessionId": "session-456",
    "attributes": {
      "environment": "production"
    },
    "numericAttributes": {
      "concurrent_connections": 5000
    },
    "booleanAttributes": {
      "is_premium": true
    },
    "serviceContext": {
      "serviceName": "session-service",
      "serviceVersion": "1.0.0",
      "environment": "production",
      "instanceId": "instance-789",
      "metrics": {
        "cpu_usage": "0.75"
      }
    }
  }'
```

### gRPC API

The service also exposes a gRPC API as defined in the `feature-flag-api.proto` file.

## Client Libraries

Client libraries are available for:

- Java
- Go
- Python

## License

This project is licensed under the MIT License - see the LICENSE file for details.

