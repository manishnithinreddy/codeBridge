# CodeBridge Gateway Service

This service combines the functionality of three previously separate services:
- API Gateway
- Gateway
- Service Discovery

## Features

### API Gateway
- Centralized entry point for all client requests
- Request routing to appropriate microservices
- Request/response transformation
- API versioning
- Request logging and monitoring
- Circuit breaking for fault tolerance
- Rate limiting to prevent abuse

### Service Discovery
- Service registration and discovery using Netflix Eureka
- Load balancing of service instances
- Health monitoring of registered services
- Dynamic scaling support

### Security
- Authentication and authorization
- JWT token validation
- API key validation
- Rate limiting
- Request filtering

## Architecture

The service is organized into modules that correspond to the original services:

```
com.codebridge.gateway
├── api - API Gateway functionality
├── discovery - Service Discovery functionality
├── config - Configuration classes
├── filter - Gateway filters
├── security - Security configurations
└── controller - REST API controllers
```

## Getting Started

### Prerequisites
- Java 17
- Maven

### Configuration
The service can be configured through application.yml or environment variables.

### Building
```bash
mvn clean package
```

### Running
```bash
java -jar target/codebridge-gateway-service-1.0.0.jar
```

## API Documentation
API documentation is available at `/swagger-ui.html` when the service is running.

