# CodeBridge Database Service

The CodeBridge Database Service provides enhanced database integration capabilities for the CodeBridge platform. It supports multiple database types, schema management, query building, and database testing.

## Features

### Multi-Database Support
- SQL databases (MySQL, PostgreSQL, Oracle, SQL Server, H2)
- NoSQL databases (MongoDB, Cassandra, Redis)
- Graph databases (Neo4j)
- Time-series databases (InfluxDB)
- Cloud database services

### Connection Management
- Create, update, and delete database connections
- Test database connections
- Connection pooling and caching
- Secure credential storage

### Schema Management
- Capture database schemas
- View table and column information
- Track schema changes over time
- Support for database migrations

### Query Building and Management
- Build and execute queries for different database types
- Parameter binding for secure queries
- Query result formatting
- Query performance monitoring

### Database Testing and Validation
- Test database connections
- Measure query performance
- Validate data against rules
- Generate test data

## API Endpoints

### Connection Management
- `GET /api/connections` - Get all connections
- `GET /api/connections/type/{type}` - Get connections by type
- `GET /api/connections/{id}` - Get connection by ID
- `POST /api/connections` - Create connection
- `PUT /api/connections/{id}` - Update connection
- `DELETE /api/connections/{id}` - Delete connection
- `GET /api/connections/{id}/test` - Test connection
- `POST /api/connections/{id}/query` - Execute query
- `POST /api/connections/{id}/update` - Execute update
- `POST /api/connections/{id}/close` - Close connection

### Schema Management
- `GET /api/schemas/connection/{connectionId}` - Get schemas by connection
- `GET /api/schemas/{id}` - Get schema by ID
- `GET /api/schemas/connection/{connectionId}/latest` - Get latest schema
- `POST /api/schemas/connection/{connectionId}/capture` - Capture schema

### Query Building
- `GET /api/query-builder/select` - Execute SELECT query
- `POST /api/query-builder/insert` - Execute INSERT query
- `PUT /api/query-builder/update` - Execute UPDATE query
- `DELETE /api/query-builder/delete` - Execute DELETE query

### Database Testing
- `GET /api/testing/connection/{connectionId}` - Test connection
- `POST /api/testing/performance/{connectionId}` - Test performance
- `POST /api/testing/validation/{connectionId}` - Test data validation

## Configuration

The service can be configured through the `application.yml` file. Key configuration options include:

- Connection pool settings
- Query execution settings
- Schema management settings
- Security settings
- Monitoring settings
- Supported database types and drivers

## Dependencies

- Spring Boot
- Spring Data JPA
- Spring Cloud (Eureka)
- Database drivers (MySQL, PostgreSQL, MongoDB, Neo4j, InfluxDB, etc.)
- Migration tools (Flyway, Liquibase)
- Caching (Caffeine)
- Monitoring (Micrometer, Prometheus)

## Getting Started

1. Configure the application.yml file with your database settings
2. Build the service: `mvn clean package`
3. Run the service: `java -jar target/codebridge-db-service-1.0.0.jar`
4. Access the API at http://localhost:8085/db/api/

## Security

The service includes several security features:

- JWT-based authentication
- Role-based access control
- Query validation and sanitization
- Connection encryption
- Secure credential storage

## Monitoring

The service exposes metrics through Spring Boot Actuator and Micrometer:

- Health endpoint: `/db/actuator/health`
- Metrics endpoint: `/db/actuator/metrics`
- Prometheus endpoint: `/db/actuator/prometheus`

