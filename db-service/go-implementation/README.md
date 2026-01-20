# DB Service (Go Implementation)

This is the Go implementation of the CodeBridge DB Service, responsible for database connection management, query execution, and schema information retrieval.

## Features

- Support for multiple database types (MySQL, PostgreSQL, SQLite)
- Connection pooling and management
- Query execution with parameter binding
- Batch query execution
- Transaction support
- Schema information retrieval
- Authentication via JWT tokens from Session Service

## Architecture

The service follows a clean architecture approach:

- **API Layer**: HTTP handlers and middleware
- **Service Layer**: Business logic
- **Database Layer**: Database connection and query execution
- **Model Layer**: Domain models and entities

## Prerequisites

- Go 1.20 or higher
- MySQL, PostgreSQL, or SQLite (depending on usage)

## Configuration

Configuration is loaded from YAML files and environment variables:

```yaml
server:
  port: 8081
  readTimeout: 10s
  writeTimeout: 10s
  idleTimeout: 60s

database:
  defaultType: sqlite  # mysql, postgres, sqlite
  maxOpenConns: 10
  maxIdleConns: 5
  connMaxLife: 5m
  
  mysql:
    host: localhost
    port: 3306
    user: root
    password: ""
    dbName: codebridge
    params: "parseTime=true&charset=utf8mb4&collation=utf8mb4_unicode_ci"
  
  postgres:
    host: localhost
    port: 5432
    user: postgres
    password: ""
    dbName: codebridge
    sslMode: disable
  
  sqlite:
    path: codebridge.db

sessionService:
  url: http://localhost:8080/api
```

Environment variables can override configuration values with the `DB_` prefix:

```bash
DB_SERVER_PORT=8082
DB_DATABASE_DEFAULTTYPE=mysql
DB_DATABASE_MYSQL_HOST=mysql.example.com
```

## Building and Running

### Local Development

```bash
# Build the service
go build -o db-service ./cmd/server

# Run the service
./db-service
```

### Docker

```bash
# Build the Docker image
docker build -t codebridge/db-service-go .

# Run the container
docker run -p 8081:8081 codebridge/db-service-go
```

## API Endpoints

All endpoints require authentication via JWT token in the Authorization header.

### Connection Management

- `POST /api/connect`: Connect to a database
- `POST /api/disconnect`: Disconnect from a database

### Query Execution

- `POST /api/query`: Execute a SQL query
- `POST /api/batch`: Execute multiple SQL queries

### Schema Information

- `POST /api/schema`: Get database schema information

### Health Check

- `GET /health`: Health check endpoint

## Scalability Considerations

This implementation is designed for horizontal scalability:

- **Connection Pooling**: Efficient database connection management
- **Configurable Limits**: Control over connection pool size
- **Stateless Design**: No local state is maintained
- **Graceful Shutdown**: Proper handling of shutdown signals
- **Health Checks**: Monitoring endpoint for load balancers

## Security Features

- **JWT Authentication**: Token-based authentication
- **Token Validation**: Validation with Session Service
- **Parameter Binding**: Protection against SQL injection
- **Query Timeouts**: Prevention of long-running queries
- **CORS Protection**: Configurable CORS headers

## Monitoring and Observability

- **Structured Logging**: JSON-formatted logs for easy parsing
- **Request Logging**: Detailed request logs
- **Health Checks**: Endpoint for monitoring service health
- **Connection Metrics**: Information about active connections

