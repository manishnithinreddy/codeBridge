# CodeBridge Platform - Deployment Guide

## 🎯 Overview

This guide provides comprehensive instructions for building, deploying, and running the CodeBridge platform with Java 21 and external database configuration.

## ✅ Build Status

All services have been successfully compiled and built with Java 21:

- ✅ **codebridge-gateway-service** - API Gateway & Service Discovery
- ✅ **codebridge-server-service** - Main Server Service
- ✅ **codebridge-teams-service** - Teams Management Service
- ✅ **codebridge-monitoring-service** - Monitoring & Metrics Service
- ✅ **codebridge-documentation-service** - Documentation Service
- ✅ **codebridge-api-test-service** - API Testing Service
- ✅ **codebridge-docker-service** - Docker Management Service
- ✅ **codebridge-security** - Security & Authentication Service

## 🛠️ Prerequisites

- **Java 21** (Oracle JDK or OpenJDK)
- **Docker** and **Docker Compose**
- **PostgreSQL 15+** (for external database)
- **RabbitMQ** (for message queuing)
- **Redis** (for caching)

## 🚀 Quick Start

### 1. Build All Services

```bash
# Make the build script executable
chmod +x build-all-services.sh

# Build all services with Java 21
./build-all-services.sh
```

### 2. Start with Docker Compose

```bash
# Start all services with external configuration
docker-compose -f docker-compose-external.yml up -d

# View logs for all services
docker-compose -f docker-compose-external.yml logs -f

# View logs for a specific service
docker-compose -f docker-compose-external.yml logs -f gateway-service
```

### 3. Access Services

| Service | Port | URL | Description |
|---------|------|-----|-------------|
| Gateway Service | 8080 | http://localhost:8080 | Main API Gateway |
| Security Service | 8080 | http://localhost:8080/security | Authentication & Authorization |
| Monitoring Service | 8081 | http://localhost:8081 | Monitoring & Metrics |
| Server Service | 8082 | http://localhost:8082/api/server | Server Management |
| Teams Service | 8083 | http://localhost:8083/teams | Team Management |
| Documentation Service | 8084 | http://localhost:8084 | API Documentation |
| API Test Service | 8085 | http://localhost:8085 | API Testing |
| Docker Service | 8086 | http://localhost:8086 | Docker Management |

## 🔧 Configuration

### External Database Configuration

Each service has been configured with external PostgreSQL databases:

- **Security Service**: `codebridge_security`
- **Server Service**: `codebridge_server`
- **Teams Service**: `codebridge_teams`
- **Monitoring Service**: `codebridge_monitoring`
- **Documentation Service**: `codebridge_documentation`
- **API Test Service**: `codebridge_api_test`
- **Docker Service**: `codebridge_docker`

### Environment Variables

Set these environment variables for production:

```bash
# Database Configuration
export DB_USERNAME=postgres
export DB_PASSWORD=your_secure_password

# RabbitMQ Configuration
export RABBITMQ_USERNAME=guest
export RABBITMQ_PASSWORD=your_secure_password

# JWT Secrets
export JWT_SECRET=your_jwt_secret_key
export JWT_SHARED_SECRET=your_shared_jwt_secret

# Jasypt Encryption
export JASYPT_ENCRYPTOR_PASSWORD=your_jasypt_password
```

### External Server Configuration

For deployment on external server (223.187.54.126), each service includes:

- **External database connections** to PostgreSQL on 223.187.54.126:5432
- **Service discovery** via Eureka on 223.187.54.126:8080
- **Inter-service communication** using external IP addresses
- **Production-optimized settings** (connection pools, timeouts, etc.)

## 📁 Project Structure

```
codeBridge/
├── codebridge-gateway-service/          # API Gateway & Service Discovery
│   ├── src/main/resources/
│   │   ├── application.yml              # Default configuration
│   │   └── application-external.yml     # External deployment config
│   └── Dockerfile
├── codebridge-server-service/           # Main Server Service
│   ├── src/main/resources/
│   │   ├── application.yml              # Default configuration
│   │   └── application-external.yml     # External deployment config
│   └── Dockerfile
├── codebridge-teams-service/            # Teams Management
├── codebridge-monitoring-service/       # Monitoring & Metrics
├── codebridge-documentation-service/    # Documentation
├── codebridge-api-test-service/         # API Testing
├── codebridge-docker-service/           # Docker Management
├── codebridge-security/                 # Security & Authentication
├── docker-compose-external.yml         # External deployment compose
├── init-databases.sql                  # Database initialization
├── build-all-services.sh               # Build script
└── README-DEPLOYMENT.md                # This file
```

## 🐳 Docker Configuration

### Services Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   PostgreSQL    │    │    RabbitMQ     │    │     Redis       │
│   (Database)    │    │ (Message Queue) │    │    (Cache)      │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
    ┌────────────────────────────┼────────────────────────────┐
    │                            │                            │
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│ Gateway Service │    │Security Service │    │ Server Service  │
│    (Port 8080)  │    │   (Port 8080)   │    │   (Port 8082)   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
    ┌────────────────────────────┼────────────────────────────┐
    │                            │                            │
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│ Teams Service   │    │Monitoring Svc   │    │Documentation Svc│
│   (Port 8083)   │    │   (Port 8081)   │    │   (Port 8084)   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
    ┌────────────────────────────┼────────────────────────────┐
    │                            │                            │
┌─────────────────┐    ┌─────────────────┐
│API Test Service │    │ Docker Service  │
│   (Port 8085)   │    │   (Port 8086)   │
└─────────────────┘    └─────────────────┘
```

### Health Checks

All services include health check endpoints:
- **Health**: `/actuator/health`
- **Metrics**: `/actuator/metrics`
- **Prometheus**: `/actuator/prometheus`

## 🔍 Monitoring & Logging

### Application Logs

Logs are stored in `/var/log/codebridge/` for each service:
- `security-service.log`
- `server-service.log`
- `teams-service.log`
- etc.

### Metrics & Monitoring

- **Prometheus metrics** available at `/actuator/prometheus`
- **Health checks** at `/actuator/health`
- **Application info** at `/actuator/info`

## 🛡️ Security Configuration

### JWT Authentication

- **Issuer URI**: `http://223.187.54.126:8080/auth/realms/codebridge`
- **JWK Set URI**: `http://223.187.54.126:8080/auth/realms/codebridge/protocol/openid-connect/certs`

### CORS Configuration

Configured to allow requests from:
- `http://223.187.54.126:3000` (Frontend)
- `http://223.187.54.126:8080` (Gateway)
- `http://localhost:3000` (Local development)
- `http://localhost:8080` (Local gateway)

## 🚨 Troubleshooting

### Common Issues

1. **Port Conflicts**
   ```bash
   # Check if ports are in use
   netstat -tulpn | grep :8080
   
   # Stop conflicting services
   docker-compose -f docker-compose-external.yml down
   ```

2. **Database Connection Issues**
   ```bash
   # Check PostgreSQL connectivity
   docker-compose -f docker-compose-external.yml logs postgres
   
   # Verify database creation
   docker exec -it codebridge-postgres psql -U postgres -l
   ```

3. **Service Discovery Issues**
   ```bash
   # Check Eureka server logs
   docker-compose -f docker-compose-external.yml logs gateway-service
   
   # Verify service registration
   curl http://localhost:8080/eureka/apps
   ```

### Log Analysis

```bash
# View all service logs
docker-compose -f docker-compose-external.yml logs

# Follow logs for specific service
docker-compose -f docker-compose-external.yml logs -f server-service

# View last 100 lines
docker-compose -f docker-compose-external.yml logs --tail=100 teams-service
```

## 📈 Performance Tuning

### Production Settings

Each service includes production-optimized configurations:

- **Connection Pools**: Increased pool sizes for database connections
- **Timeouts**: Appropriate timeout values for external calls
- **Circuit Breakers**: Resilience4j configuration for fault tolerance
- **Rate Limiting**: Bucket4j configuration for API rate limiting
- **Caching**: Caffeine cache configuration for performance

### Resource Requirements

Minimum recommended resources:

| Service | CPU | Memory | Storage |
|---------|-----|--------|---------|
| Gateway Service | 0.5 CPU | 512MB | 1GB |
| Security Service | 0.5 CPU | 512MB | 2GB |
| Server Service | 1 CPU | 1GB | 5GB |
| Teams Service | 0.5 CPU | 512MB | 2GB |
| Monitoring Service | 0.5 CPU | 512MB | 3GB |
| Documentation Service | 0.5 CPU | 512MB | 2GB |
| API Test Service | 0.5 CPU | 512MB | 2GB |
| Docker Service | 1 CPU | 1GB | 10GB |
| **Total** | **5 CPU** | **5.5GB** | **27GB** |

## 🎉 Success!

Your CodeBridge platform is now ready for deployment with:

- ✅ **Java 21** compatibility
- ✅ **External database** configuration
- ✅ **Docker containerization**
- ✅ **Production-ready** settings
- ✅ **Comprehensive monitoring**
- ✅ **Security configuration**
- ✅ **Service discovery**
- ✅ **Load balancing**

For support or questions, please refer to the individual service documentation or contact the development team.
