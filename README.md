# CodeBridge Platform

CodeBridge is a cross-platform plugin ecosystem that integrates daily-use tools and features directly into your IDE, reducing context switching and improving developer productivity.

## 🚀 Services Overview

The CodeBridge platform consists of multiple microservices:

### Java Services (Spring Boot)
- **Gateway Service** (Port 8080) - API Gateway and routing
- **Docker Service** (Port 8082) - Docker container management
- **GitLab Service** (Port 8086) - GitLab integration and Git operations
- **Documentation Service** (Port 8087) - Documentation management
- **Server Service** (Port 8088) - Server management and SSH access

### Go Services
- **Session Service** (Port 8083) - Session management
- **DB Service** (Port 8084) - Database operations

### Python Services
- **AI Service** (Port 8085) - AI-powered database query conversion

### Infrastructure
- **PostgreSQL** (Port 5432) - Primary database
- **Redis** (Port 6379) - Caching and session storage

## 📋 Prerequisites

- **Java 21** (OpenJDK or Oracle JDK)
- **Maven 3.8+**
- **Docker & Docker Compose**
- **Go 1.19+** (for Go services)
- **Python 3.9+** (for AI service)

## 🛠️ Building the Project

### Quick Build

Use the provided build script to compile all Java services:

```bash
chmod +x build-services.sh
./build-services.sh
```

### Manual Build

1. **Set Java Version** (using SDKMAN):
```bash
sdk use java 21.0.5-tem
```

2. **Build Core Services**:
```bash
mvn clean compile -DskipTests -pl codebridge-common,codebridge-core,codebridge-security,codebridge-gateway-service,codebridge-gitlab-service,codebridge-docker-service,codebridge-documentation-service,codebridge-server-service
```

3. **Package JAR Files**:
```bash
mvn clean package -DskipTests -Dmaven.test.skip=true -pl codebridge-common,codebridge-core,codebridge-security,codebridge-gateway-service,codebridge-gitlab-service,codebridge-docker-service,codebridge-documentation-service,codebridge-server-service
```

## 🐳 Running with Docker

### Start All Services

```bash
docker-compose up -d
```

### Start Specific Services

```bash
# Start only infrastructure
docker-compose up -d postgres redis

# Start Java services
docker-compose up -d gateway-service docker-service gitlab-service documentation-service server-service

# Start Go services
docker-compose up -d session-service db-service

# Start Python services
docker-compose up -d ai-service
```

### View Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f gateway-service
```

### Stop Services

```bash
docker-compose down
```

## 🔧 Running Individual Services

### Java Services

```bash
# Gateway Service
java -jar codebridge-gateway-service/target/codebridge-gateway-service-3.2.0.jar

# Docker Service
java -jar codebridge-docker-service/target/codebridge-docker-service-0.0.1-SNAPSHOT.jar

# GitLab Service
java -jar codebridge-gitlab-service/target/codebridge-gitlab-service-0.0.1-SNAPSHOT.jar

# Documentation Service
java -jar codebridge-documentation-service/target/codebridge-documentation-service-0.0.1-SNAPSHOT.jar

# Server Service
java -jar codebridge-server-service/target/codebridge-server-service-0.0.1-SNAPSHOT.jar
```

### Environment Variables

Each service can be configured using environment variables:

```bash
# Database Configuration
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/codebridge
SPRING_DATASOURCE_USERNAME=codebridge
SPRING_DATASOURCE_PASSWORD=codebridge

# Redis Configuration
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6379

# Profile
SPRING_PROFILES_ACTIVE=prod
```

## 🗄️ Database Setup

The PostgreSQL database is automatically initialized with:

- **codebridge_git** - Git operations data
- **codebridge_docker** - Docker container data
- **codebridge_server** - Server management data
- **codebridge_api** - API testing data

UUID extensions are enabled for all databases.

## 🔍 Health Checks

All services include health check endpoints:

- Gateway Service: http://localhost:8080/actuator/health
- Docker Service: http://localhost:8082/actuator/health
- GitLab Service: http://localhost:8086/actuator/health
- Documentation Service: http://localhost:8087/actuator/health
- Server Service: http://localhost:8088/actuator/health

## 🏗️ Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Gateway       │    │   Docker        │    │   GitLab        │
│   Service       │    │   Service       │    │   Service       │
│   (Port 8080)   │    │   (Port 8082)   │    │   (Port 8086)   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  Documentation  │    │   Server        │    │   Session       │
│   Service       │    │   Service       │    │   Service (Go)  │
│   (Port 8087)   │    │   (Port 8088)   │    │   (Port 8083)   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
┌─────────────────┐    ┌─────────────────┐
│   DB Service    │    │   AI Service    │
│   (Go)          │    │   (Python)      │
│   (Port 8084)   │    │   (Port 8085)   │
└─────────────────┘    └─────────────────┘
         │                       │
         └───────────────────────┘
                 │
    ┌─────────────────┐    ┌─────────────────┐
    │   PostgreSQL    │    │     Redis       │
    │   (Port 5432)   │    │   (Port 6379)   │
    └─────────────────┘    └─────────────────┘
```

## 🔧 Development

### Adding New Services

1. Create service directory following the naming convention
2. Add service configuration to `docker-compose.yml`
3. Update build scripts and documentation
4. Ensure proper health checks and logging

### Testing

```bash
# Run tests for specific service
mvn test -pl codebridge-gateway-service

# Run all tests
mvn test
```

## 📝 Features

- **GitLab Integration** - Repository management, CI/CD pipelines
- **Docker Management** - Container lifecycle, image management
- **API Testing** - REST API testing and validation
- **Server Session Management** - Secure server access without credential sharing
- **AI Database Agent** - Natural language to SQL query conversion
- **Team Collaboration** - Shared access and team management
- **Documentation** - Integrated documentation management

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🆘 Troubleshooting

### Common Issues

1. **Java Version Mismatch**: Ensure Java 21 is installed and active
2. **Port Conflicts**: Check if ports are already in use
3. **Docker Issues**: Ensure Docker daemon is running
4. **Database Connection**: Verify PostgreSQL is running and accessible

### Logs

Check service logs for detailed error information:

```bash
# Docker logs
docker-compose logs [service-name]

# Application logs
tail -f logs/application.log
```

For more help, please check the documentation or create an issue in the repository.

