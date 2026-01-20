# CodeBridge Performance Service

The CodeBridge Performance Service provides comprehensive performance monitoring, alerting, and optimization capabilities for the CodeBridge platform.

## Features

### Metrics Collection Infrastructure
- **Performance Metrics Collector**: Central metrics collection and aggregation
- **API Metrics Collector**: API-specific metrics (response times, error rates)
- **SSH Metrics Collector**: SSH operation metrics (connection times, command execution)
- **Database Metrics Collector**: Database query metrics (query times, connection pool stats)
- **Resource Metrics Collector**: System resource metrics (CPU, memory, disk, threads)
- **Client Metrics Collection**: Client-side performance metrics (page load times, network metrics)

### Performance Visualization and Alerting
- **Alerting System**: Configurable alerts based on thresholds
- **Anomaly Detection**: Statistical anomaly detection for metrics
- **Trend Analysis**: Analyze trends in performance metrics
- **Baseline Comparison**: Compare current performance to historical baselines

### Automated Performance Testing
- **Performance Test Framework**: Automated performance test execution
- **Performance Regression Testing**: Detect performance regressions
- **Resource Optimization**: Identify and optimize resource usage
- **Continuous Performance Monitoring**: Real-time performance tracking and SLA monitoring

## Architecture

The service is built using Spring Boot and integrates with:
- InfluxDB for time-series data storage
- Prometheus for metrics collection
- Micrometer for metrics instrumentation
- OpenTelemetry for distributed tracing

## Configuration

The service is configured via `application.yml` with the following key sections:

```yaml
performance:
  metrics:
    collection:
      enabled: true
      interval: 15000  # 15 seconds
      retention-days: 30
    influxdb:
      url: http://localhost:8086
      token: your_influxdb_token
      org: codebridge
      bucket: performance_metrics
  alerting:
    enabled: true
    check-interval: 60000  # 1 minute
  anomaly-detection:
    enabled: true
    sensitivity: 2.0  # Standard deviations for anomaly threshold
  testing:
    enabled: true
    schedule: "0 0 2 * * *"  # 2 AM daily
  dashboard:
    refresh-interval: 30000  # 30 seconds
  client-metrics:
    sampling-rate: 0.1  # 10% of requests
```

## API Endpoints

The service exposes the following key API endpoints:

- `/api/metrics`: Metrics management
- `/api/metrics/client`: Client-side metrics collection
- `/api/alerts`: Alert management
- `/api/tests`: Performance test management
- `/api/sla`: SLA management
- `/api/optimization`: Resource optimization recommendations

## Getting Started

### Prerequisites
- Java 17 or higher
- Maven 3.6 or higher
- InfluxDB (optional, for time-series storage)
- Prometheus (optional, for metrics collection)

### Building
```bash
mvn clean install
```

### Running
```bash
java -jar target/codebridge-performance-service-1.0.0.jar
```

### Docker
```bash
docker build -t codebridge-performance-service .
docker run -p 8086:8086 codebridge-performance-service
```

## Integration with Other Services

The Performance Service integrates with other CodeBridge services:
- **API Test Service**: Collects metrics from API tests
- **SSH Service**: Monitors SSH operation performance
- **Database Service**: Tracks database query performance
- **Gateway Service**: Monitors API gateway performance

## License

This project is licensed under the MIT License - see the LICENSE file for details.

