# CodeBridge - Microservices Platform

## 🎉 Status: All Compilation Issues Fixed!

This repository contains a microservices-based platform for code collaboration and GitLab integration. All compilation and runtime issues have been resolved, and both services are now building and running successfully with Java 21.

## 🏗️ Architecture

The platform consists of two main services:

1. **Gateway Service** (Port 8080) - API Gateway with routing and load balancing
2. **GitLab Service** (Port 8081) - GitLab integration and Git operations

## ✅ Fixed Issues

### 1. Entity Builder Pattern Issues
- **Problem**: Multiple entity classes had builder pattern inheritance issues
- **Solution**: Replaced `@Builder` with `@SuperBuilder` in all entity classes extending `BaseEntity`
- **Affected Classes**: `Webhook`, `Repository`, `GitProvider`, `SharedStash`, `GitCredential`

### 2. Security Configuration Conflicts
- **Problem**: Two conflicting security configurations causing bean definition errors
- **Solution**: Removed duplicate `SecurityConfig.java` and streamlined `GitLabSecurityConfig.java`
- **Development Mode**: OAuth2 temporarily disabled for easier development

### 3. Service Discovery Issues
- **Problem**: Eureka client trying to register with non-existent server
- **Solution**: Disabled Eureka for GitLab service in development mode
- **Configuration**: Added proper Eureka disable settings in `application.yml`

### 4. MapStruct Compilation Issues
- **Problem**: Mapper generation failures due to entity inheritance
- **Solution**: Fixed with `@SuperBuilder` pattern and proper inheritance setup

### 5. Docker Configuration
- **Problem**: Port mismatches between application config and Dockerfile
- **Solution**: Updated Dockerfile to match actual application ports and health check paths

## 🚀 Quick Start

### Prerequisites
- Java 21 (OpenJDK or Oracle JDK)
- Maven 3.6+
- Docker & Docker Compose (optional)
- PostgreSQL (for GitLab service)

### Option 1: Run with Docker Compose (Recommended)

```bash
# Build and start all services
docker compose up --build

# Or run in detached mode
docker compose up --build -d

# View logs
docker compose logs -f

# Stop services
docker compose down
```

This will start:
- Gateway Service on http://localhost:8080
- GitLab Service on http://localhost:8081
- PostgreSQL database on localhost:5432

### Option 2: Run Services Individually

#### 1. Start PostgreSQL Database
```bash
# Using Docker
docker run -d \
  --name postgres-codebridge \
  -e POSTGRES_DB=codebridge_git \
  -e POSTGRES_USER=codebridge \
  -e POSTGRES_PASSWORD=codebridge \
  -p 5432:5432 \
  postgres:15-alpine

# Or install PostgreSQL locally and create the database
```

#### 2. Build and Run Gateway Service
```bash
cd codebridge-gateway-service
mvn clean package -DskipTests
java -jar target/codebridge-gateway-service-3.2.0.jar
```

#### 3. Build and Run GitLab Service
```bash
cd codebridge-gitlab-service
mvn clean package -DskipTests
java -jar target/codebridge-gitlab-service-0.0.1-SNAPSHOT.jar
```

## 🔧 Configuration

### Gateway Service
- **Port**: 8080
- **Health Check**: http://localhost:8080/actuator/health
- **Eureka**: Enabled (connects to service registry)

### GitLab Service
- **Port**: 8081
- **Context Path**: `/api/gitlab`
- **Health Check**: http://localhost:8081/api/gitlab/actuator/health
- **Database**: PostgreSQL (codebridge_git)
- **Eureka**: Disabled for development

## 📊 Service Endpoints

### Gateway Service
- Health: `GET http://localhost:8080/actuator/health`
- Routes: Configured to proxy requests to backend services

### GitLab Service
- Health: `GET http://localhost:8081/api/gitlab/actuator/health`
- API Docs: `GET http://localhost:8081/api/gitlab/swagger-ui.html`
- GitLab API: Various endpoints under `/api/gitlab/`

## 🛠️ Development

### Building Services
```bash
# Build all services
mvn clean package -DskipTests

# Build specific service
cd codebridge-gateway-service
mvn clean package -DskipTests
```

### Running Tests
```bash
# Run tests for all services
mvn test

# Run tests for specific service
cd codebridge-gitlab-service
mvn test
```

### Development Profiles
Both services are configured with `dev` profile by default, which includes:
- Debug logging for application packages
- Relaxed security settings
- Local database connections

## 🐳 Docker

### Individual Service Images
```bash
# Build Gateway Service image
cd codebridge-gateway-service
docker build -t codebridge-gateway .

# Build GitLab Service image
cd codebridge-gitlab-service
docker build -t codebridge-gitlab .
```

### Docker Compose Services
The `docker-compose.yml` includes:
- **codebridge-gateway**: Gateway service with health checks
- **codebridge-gitlab**: GitLab service with database dependency
- **postgres**: PostgreSQL database with persistent storage

## 🔍 Troubleshooting

### Common Issues

1. **Port Already in Use**
   ```bash
   # Check what's using the port
   lsof -i :8080
   lsof -i :8081
   
   # Kill the process or change port in application.yml
   ```

2. **Database Connection Issues**
   ```bash
   # Ensure PostgreSQL is running
   docker ps | grep postgres
   
   # Check database connectivity
   psql -h localhost -U codebridge -d codebridge_git
   ```

3. **Java Version Issues**
   ```bash
   # Verify Java 21 is being used
   java -version
   echo $JAVA_HOME
   
   # Set JAVA_HOME if needed
   export JAVA_HOME=/path/to/jdk-21
   ```

### Build Warnings
Some build warnings are expected and don't affect functionality:
- `@SuperBuilder` warnings about initializing expressions
- `Field 'log' already exists` warnings from Lombok

## 📝 API Documentation

Once services are running:
- Gateway Service: http://localhost:8080/actuator/health
- GitLab Service API Docs: http://localhost:8081/api/gitlab/swagger-ui.html

## 🤝 Contributing

1. Ensure Java 21 is installed
2. Run `mvn clean package` to verify builds
3. Test services individually before Docker deployment
4. Follow existing code patterns and security configurations

## 📄 License

[Add your license information here]

---

**Note**: This setup is configured for development. For production deployment, ensure proper security configurations, database credentials, and service discovery setup.

