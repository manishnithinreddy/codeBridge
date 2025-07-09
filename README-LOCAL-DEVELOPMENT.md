# CodeBridge Local Development Setup

This guide explains how to run the CodeBridge services locally for development using the fixed configuration.

## 🚀 Quick Start

### Prerequisites
- Java 21 (OpenJDK or Oracle JDK)
- Maven 3.6+
- Docker & Docker Compose (optional)

### Option 1: Run with Docker Compose (Recommended)

1. **Build all services:**
```bash
# Build Gateway Service
cd codebridge-gateway-service
mvn clean package -DskipTests
cd ..

# Build Teams Service
cd codebridge-teams-service
mvn clean package -DskipTests
cd ..

# Build Server Service
cd codebridge-server-service
mvn clean package -DskipTests
cd ..
```

2. **Start all services with Docker:**
```bash
docker-compose -f docker-compose.local.yml up --build
```

3. **Access the services:**
- **Gateway**: http://localhost:8080
- **Teams API**: http://localhost:8082/teams
- **Server API**: http://localhost:8081/api/server

### Option 2: Run Manually

1. **Build all services** (same as above)

2. **Start each service in separate terminals:**

**Terminal 1 - Teams Service:**
```bash
cd codebridge-teams-service
java -jar target/codebridge-teams-service-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

**Terminal 2 - Server Service:**
```bash
cd codebridge-server-service
java -jar target/codebridge-server-service-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

**Terminal 3 - Gateway Service:**
```bash
cd codebridge-gateway-service
java -jar target/codebridge-gateway-service-3.2.0.jar --spring.profiles.active=local
```

## 🔧 Configuration Details

### Service Ports
- **Gateway Service**: 8080 (Entry point)
- **Teams Service**: 8082
- **Server Service**: 8081

### Database Configuration
All services use **embedded H2 databases** for local development:
- **Teams Service**: `jdbc:h2:mem:teams-db`
- **Server Service**: `jdbc:h2:mem:server-db`

### H2 Console Access
- **Teams Service**: http://localhost:8082/teams/h2-console
- **Server Service**: http://localhost:8081/api/server/h2-console

**H2 Console Login:**
- JDBC URL: `jdbc:h2:mem:teams-db` (or `server-db`)
- User Name: `sa`
- Password: (leave empty)

### JWT Configuration
All services use a shared JWT secret for local development:
```
local-development-jwt-secret-that-is-long-enough-for-hmac-sha-256-algorithm
```

### CORS Configuration
Gateway service is configured to allow requests from:
- http://localhost:3000 (React dev server)
- http://localhost:4200 (Angular dev server)
- http://127.0.0.1:3000
- http://127.0.0.1:4200

## 🛠️ Development Features

### Health Checks
All services expose health check endpoints:
- **Gateway**: http://localhost:8080/actuator/health
- **Teams**: http://localhost:8082/teams/actuator/health
- **Server**: http://localhost:8081/api/server/actuator/health

### API Documentation
Services expose Swagger/OpenAPI documentation:
- **Gateway**: http://localhost:8080/swagger-ui.html
- **Teams**: http://localhost:8082/teams/swagger-ui.html
- **Server**: http://localhost:8081/api/server/swagger-ui.html

### Logging
All services are configured with DEBUG level logging for development:
- Application logs: `DEBUG`
- Spring Cloud Gateway: `DEBUG`
- Spring Security: `INFO`

## 🔍 Troubleshooting

### Common Issues

1. **Port Already in Use**
   ```bash
   # Check what's using the port
   lsof -i :8080
   # Kill the process
   kill -9 <PID>
   ```

2. **Java Version Issues**
   ```bash
   # Check Java version
   java -version
   # Should show Java 21
   ```

3. **Maven Build Issues**
   ```bash
   # Clean and rebuild
   mvn clean install -DskipTests
   ```

4. **Docker Issues**
   ```bash
   # Clean Docker containers and images
   docker-compose -f docker-compose.local.yml down
   docker system prune -f
   ```

### Service Dependencies
- Gateway service depends on Teams and Server services
- Start Teams and Server services first, then Gateway
- All services are configured to retry connections

### Database Reset
To reset the H2 databases, simply restart the services. All data is stored in memory and will be lost on restart.

## 📝 API Testing

### Sample API Calls

**Health Check:**
```bash
curl http://localhost:8080/actuator/health
```

**Teams API (via Gateway):**
```bash
curl http://localhost:8080/teams/api/teams
```

**Server API (via Gateway):**
```bash
curl http://localhost:8080/api/server/servers
```

**Direct Service Access:**
```bash
# Teams Service directly
curl http://localhost:8082/teams/api/teams

# Server Service directly
curl http://localhost:8081/api/server/servers
```

## 🚦 Service Status

All services should start successfully with the following indicators:

✅ **Gateway Service**: 
- Started on port 8080
- Routes configured for backend services
- CORS enabled

✅ **Teams Service**: 
- Started on port 8082
- H2 database initialized
- JWT security configured

✅ **Server Service**: 
- Started on port 8081
- H2 database initialized
- JWT security configured

## 🔄 Next Steps

1. **Frontend Integration**: Configure your frontend to use http://localhost:8080 as the API base URL
2. **Authentication**: Implement JWT token generation and validation
3. **Database Migration**: When ready for production, switch to PostgreSQL
4. **Service Discovery**: Add Eureka for production deployment
5. **Message Queue**: Add RabbitMQ for asynchronous processing

---

**Note**: This setup is optimized for local development. For production deployment, use the main `docker-compose.yml` with PostgreSQL and Redis.
