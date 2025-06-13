# CodeBridge Monitoring Service

This service combines the functionality of three previously separate services:
- Performance Service
- Platform Operations
- Scalability and High Availability

## Features

### Performance Monitoring
- API performance metrics collection and analysis
- Database query performance monitoring
- Resource utilization tracking
- SSH session performance metrics
- Alerting system for performance degradation
- Anomaly detection for performance metrics
- SLA monitoring and reporting
- Time series analysis of performance data

### Platform Operations
- Admin dashboard for system management
- System health monitoring
- Audit logging for administrative actions
- Webhook management for external integrations
- Event handling and processing
- Export capabilities for logs and metrics
- Reporting and analytics

### Scalability and High Availability
- Auto-scaling capabilities
- Load balancing strategies
- Data resilience and replication
- Session management
- Idempotency handling
- Health check services
- Circuit breaking and fault tolerance

## Architecture

The service is organized into three main modules that correspond to the original services:

```
com.codebridge.monitoring
├── performance - Performance monitoring functionality
├── platform - Platform operations functionality
├── scalability - Scalability and high availability functionality
├── common - Shared utilities and components
├── config - Configuration classes
├── controller - REST API controllers
├── service - Service implementations
├── repository - Data access layer
├── model - Domain models
└── dto - Data transfer objects
```

## Getting Started

### Prerequisites
- Java 17
- Maven
- PostgreSQL database
- Redis (for distributed caching)

### Configuration
The service can be configured through application.yml or environment variables.

### Building
```bash
mvn clean package
```

### Running
```bash
java -jar target/codebridge-monitoring-service-1.0.0.jar
```

## API Documentation
API documentation is available at `/swagger-ui.html` when the service is running.

