# CodeBridge Monitoring Service

A comprehensive monitoring, performance analysis, and platform operations service for the CodeBridge platform. This service combines functionality from the previous monitoring and performance services.

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
- Client-side metrics collection
- Performance test framework
- Performance regression testing
- Resource optimization recommendations

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
│   ├── collector - Metrics collection components
│   ├── service - Performance analysis services
│   ├── repository - Data access for performance metrics
│   ├── model - Performance domain models
│   ├── controller - Performance API endpoints
│   └── dto - Performance data transfer objects
├── platform - Platform operations functionality
│   ├── controller - Platform operations endpoints
│   ├── service - Platform operations services
│   └── dto - Platform operations data transfer objects
├── scalability - Scalability and high availability functionality
│   ├── service - Scalability services
│   ├── config - Scalability configuration
│   └── filter - Scalability filters
├── common - Shared utilities and components
├── config - Configuration classes
├── controller - REST API controllers
├── service - Service implementations
├── repository - Data access layer
├── model - Domain models
└── dto - Data transfer objects
```

## API Endpoints

### Performance Monitoring
- `/api/metrics` - Metrics management
- `/api/metrics/client` - Client-side metrics collection
- `/api/alerts` - Alert management
- `/api/tests` - Performance test management
- `/api/sla` - SLA management
- `/api/optimization` - Resource optimization recommendations

### Platform Operations
- `/api/admin` - Admin dashboard endpoints
- `/api/health` - System health endpoints
- `/api/audit` - Audit logging endpoints
- `/api/webhooks` - Webhook management endpoints
- `/api/events` - Event handling endpoints
- `/api/export` - Export capabilities endpoints
- `/api/reports` - Reporting and analytics endpoints

### Scalability and High Availability
- `/api/scaling` - Auto-scaling endpoints
- `/api/loadbalancing` - Load balancing endpoints
- `/api/resilience` - Data resilience endpoints
- `/api/sessions` - Session management endpoints
- `/api/idempotency` - Idempotency handling endpoints
- `/api/healthcheck` - Health check endpoints
- `/api/circuitbreaker` - Circuit breaking endpoints

## Getting Started

### Prerequisites
- Java 17
- Maven
- PostgreSQL database
- Redis (for distributed caching)
- InfluxDB (for time-series data storage)

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

## Integration with Other Services

The Monitoring Service integrates with other CodeBridge services:
- **API Test Service**: Collects metrics from API tests
- **SSH Service**: Monitors SSH operation performance
- **Database Service**: Tracks database query performance
- **Gateway Service**: Monitors API gateway performance

