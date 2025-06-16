# CodeBridge Feature Flag Service

The Feature Flag Service is a central component of the CodeBridge microservices architecture, enabling dynamic service implementation switching and feature toggling.

## Overview

This service provides a robust feature flag management system that allows for:

- Dynamic switching between service implementations (Java, Go, Python)
- Context-based flag evaluation
- Real-time flag updates via streaming
- Percentage-based traffic splitting
- Rule-based flag evaluation

## Key Features

- **Multiple Interfaces**: REST API and gRPC for language-agnostic integration
- **Real-time Updates**: Stream flag changes to clients
- **Context-based Evaluation**: Evaluate flags based on user, service, and custom attributes
- **Rule Engine**: Define complex rules for flag evaluation
- **Percentage-based Routing**: Split traffic between implementations
- **Namespaces**: Organize flags by namespace
- **Redis Backend**: High-performance storage and pub/sub capabilities

## Architecture

The Feature Flag Service is built with:

- Spring Boot for the REST API
- gRPC for high-performance service-to-service communication
- Redis for storage and pub/sub messaging
- Caching for high-performance flag retrieval

## API Endpoints

### REST API

- `GET /api/v1/flags/{key}` - Get a flag by key
- `GET /api/v1/flags?keys=key1,key2` - Get multiple flags
- `POST /api/v1/flags/{key}/evaluate` - Evaluate a flag with context
- `POST /api/v1/flags` - Create or update a flag
- `PUT /api/v1/flags/{key}` - Update a flag
- `DELETE /api/v1/flags/{key}` - Delete a flag
- `GET /api/v1/flags/list` - List flags with pagination

### gRPC API

- `GetFlag` - Get a flag by key
- `GetFlags` - Get multiple flags
- `EvaluateFlag` - Evaluate a flag with context
- `SetFlag` - Create or update a flag
- `StreamFlagUpdates` - Stream flag updates in real-time
- `ListFlags` - List flags with pagination
- `HealthCheck` - Check service health

## Usage Examples

### Java Client

```java
// Create a feature flag client
FeatureFlagClient client = new FeatureFlagClient("localhost", 9090);

// Get a flag
FeatureFlag flag = client.getFlag("service-implementation", "default");

// Evaluate a flag with context
EvaluationContext context = EvaluationContext.builder()
    .userId("user-123")
    .attribute("environment", "production")
    .numericAttribute("concurrent_connections", 5000)
    .booleanAttribute("is_premium", true)
    .serviceContext(ServiceContext.builder()
        .serviceName("session-service")
        .serviceVersion("1.0.0")
        .environment("production")
        .build())
    .build();

EvaluationResult result = client.evaluateFlag("session-service.implementation", "default", context);

// Use the result
if (result.getValue().getStringValue().equals("go")) {
    // Use Go implementation
} else {
    // Use Java implementation
}
```

### Go Client

```go
// Create a feature flag client
client := featureflag.NewClient("localhost:9090")

// Evaluate a flag with context
context := &featureflag.EvaluationContext{
    UserId: "user-123",
    Attributes: map[string]string{
        "environment": "production",
    },
    NumericAttributes: map[string]float64{
        "concurrent_connections": 5000,
    },
    BooleanAttributes: map[string]bool{
        "is_premium": true,
    },
    ServiceContext: &featureflag.ServiceContext{
        ServiceName: "session-service",
        ServiceVersion: "1.0.0",
        Environment: "production",
    },
}

result, err := client.EvaluateFlag("session-service.implementation", "default", context)
if err != nil {
    log.Fatalf("Error evaluating flag: %v", err)
}

// Use the result
if result.StringValue == "go" {
    // Use Go implementation
} else {
    // Use Java implementation
}
```

## Configuration

The service can be configured using environment variables or application.yml:

```yaml
server:
  port: 8090
  servlet:
    context-path: /feature-flag

spring:
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:}
    database: ${REDIS_DATABASE:0}

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

## Deployment

The service can be deployed using Docker:

```bash
docker build -t codebridge/feature-flag-service .
docker run -p 8090:8090 -p 9090:9090 -e REDIS_HOST=redis codebridge/feature-flag-service
```

Or using Kubernetes:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: feature-flag-service
spec:
  replicas: 2
  selector:
    matchLabels:
      app: feature-flag-service
  template:
    metadata:
      labels:
        app: feature-flag-service
    spec:
      containers:
      - name: feature-flag-service
        image: codebridge/feature-flag-service:latest
        ports:
        - containerPort: 8090
        - containerPort: 9090
        env:
        - name: REDIS_HOST
          value: redis
```

## Client Libraries

Client libraries are available for:

- Java: `feature-flag-client-java`
- Go: `feature-flag-client-go`
- Python: `feature-flag-client-python`

These libraries provide a simple interface for interacting with the Feature Flag Service.

