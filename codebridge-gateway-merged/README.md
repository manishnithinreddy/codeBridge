# CodeBridge Unified Gateway Service

This service provides a consolidated API Gateway for the CodeBridge platform, combining the functionality of multiple gateway services into a single, unified entry point.

## Features

- **Unified Routing**: Single entry point for all CodeBridge microservices
- **Security**: OAuth2/JWT authentication and authorization
- **Rate Limiting**: Configurable rate limiting with Redis backend
- **Circuit Breaking**: Resilience4j circuit breakers for service protection
- **Request Logging**: Comprehensive request/response logging with correlation IDs
- **API Documentation**: Aggregated Swagger/OpenAPI documentation
- **CORS Support**: Configurable CORS settings
- **Fallback Responses**: Graceful degradation when services are unavailable

## Architecture

The Unified Gateway Service acts as the entry point for all client requests to the CodeBridge platform. It routes requests to the appropriate microservices based on path patterns and provides cross-cutting concerns like security, rate limiting, and monitoring.

## Configuration

The gateway is configured through the `application.yml` file, which includes:

- Route definitions for all microservices
- Security settings
- Rate limiting configuration
- Circuit breaker settings
- CORS configuration

## Dependencies

- Java 21
- Spring Boot 3.1.0
- Spring Cloud Gateway
- Spring Security OAuth2
- Redis (for rate limiting)
- Resilience4j
- Spring Cloud Netflix Eureka Client

## Building and Running

### Prerequisites

- JDK 21
- Maven 3.8+
- Redis server (for rate limiting)

### Build

```bash
mvn clean package
```

### Run

```bash
java -jar target/codebridge-gateway-0.1.0-SNAPSHOT.jar
```

### Docker

```bash
docker build -t codebridge/gateway .
docker run -p 8080:8080 codebridge/gateway
```

## API Documentation

The API documentation is available at:

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/api-docs

## Monitoring

The gateway exposes various metrics and health endpoints:

- Health: http://localhost:8080/actuator/health
- Metrics: http://localhost:8080/actuator/metrics
- Prometheus: http://localhost:8080/actuator/prometheus

