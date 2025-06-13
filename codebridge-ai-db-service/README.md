# CodeBridge AI-Enhanced Database Service

This service combines the functionality of two previously separate services:
- DB Service
- AI DB Agent Service

## Features

### Database Management
- Multi-database support (SQL, NoSQL, Graph, Time-series)
- Connection pooling and management
- Database migration and versioning
- Query optimization and caching
- Transaction management
- Data validation and transformation
- Schema management

### AI-Enhanced Capabilities
- Natural language to SQL conversion
- Intelligent query generation
- Schema analysis and recommendations
- Query performance optimization
- Anomaly detection in data patterns
- Automated data classification
- Context-aware query suggestions

## Supported Databases

### SQL Databases
- PostgreSQL
- MySQL
- H2 (for development/testing)

### NoSQL Databases
- MongoDB
- Cassandra
- Redis

### Graph Databases
- Neo4j

### Time-series Databases
- InfluxDB

## Architecture

The service is organized into modules that correspond to the original services:

```
com.codebridge.aidb
├── agent - AI DB Agent functionality
├── db - Database service functionality
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
- One or more of the supported databases

### Configuration
The service can be configured through application.yml or environment variables.

### Building
```bash
mvn clean package
```

### Running
```bash
java -jar target/codebridge-ai-db-service-1.0.0.jar
```

## API Documentation
API documentation is available at `/swagger-ui.html` when the service is running.

