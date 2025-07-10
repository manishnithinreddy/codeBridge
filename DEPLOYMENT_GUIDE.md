# CodeBridge Deployment Guide

## 🚀 Quick Start

All services have been successfully compiled with **Java 21** and are ready for deployment!

### Prerequisites

- Docker and Docker Compose installed
- Java 21 (already configured)
- At least 8GB RAM for full deployment
- Ports 8080-8089, 5432, and 6379 available

### Build All Services

```bash
# Run the automated build script
./build-all-services.sh
```

### Deploy with Docker Compose

```bash
# Build and start all services
docker compose up --build

# Or run in detached mode
docker compose up --build -d

# View logs
docker compose logs -f

# Stop all services
docker compose down
```

## 📋 Service Architecture

### Java Services (Port Mapping)
- **Gateway Service**: `8080` - Main API gateway
- **Docker Service**: `8082` - Container management
- **GitLab Service**: `8086` - GitLab integration
- **Documentation Service**: `8087` - Documentation management
- **Server Service**: `8088` - Server operations
- **Teams Service**: `8089` - Team management
- **Monitoring Service**: `8090` - System monitoring
- **API Test Service**: `8091` - API testing

### Go Services
- **Session Service**: `8083` - Session management
- **DB Service**: `8084` - Database operations

### Python Services
- **AI Service**: `8085` - AI/ML operations

### Infrastructure
- **PostgreSQL**: `5432` - Primary database
- **Redis**: `6379` - Caching and sessions

## 🔧 Configuration

### Environment Variables

All services are configured with:
- `SPRING_PROFILES_ACTIVE=prod`
- Database: `postgresql://codebridge:codebridge@postgres:5432/codebridge`
- Redis: `redis:6379`

### Database Initialization

The PostgreSQL database is automatically initialized with `init-db.sql`.

## 🐳 Docker Configuration

### Individual Service Build

```bash
# Build specific service
docker build -t codebridge-gitlab-service ./codebridge-gitlab-service

# Run specific service
docker run -p 8086:8086 codebridge-gitlab-service
```

### Docker Compose Files

- `docker-compose.yml` - Main production configuration
- `docker-compose.local.yml` - Local development
- `docker-compose-external.yml` - External services

## 🔍 Health Checks

All services include health check endpoints:
- `http://localhost:8080/actuator/health` (Gateway)
- `http://localhost:8086/actuator/health` (GitLab)
- And so on for each service...

## 🚨 Troubleshooting

### Common Issues

1. **Port Conflicts**: Ensure ports 8080-8089, 5432, 6379 are available
2. **Memory Issues**: Increase Docker memory allocation to 8GB+
3. **Build Failures**: Run `./build-all-services.sh` to rebuild

### Service Dependencies

Services start in this order:
1. PostgreSQL & Redis (Infrastructure)
2. DB Service & Session Service
3. Core Java services
4. Gateway Service (last)

### Logs and Debugging

```bash
# View all logs
docker compose logs

# View specific service logs
docker compose logs gateway-service

# Follow logs in real-time
docker compose logs -f gitlab-service
```

## 🎯 Testing

### Service Endpoints

Once deployed, test the services:

```bash
# Gateway health check
curl http://localhost:8080/actuator/health

# GitLab service health check
curl http://localhost:8086/actuator/health

# Documentation service health check
curl http://localhost:8087/actuator/health
```

### Load Testing

Use the included JMeter scripts:
```bash
./run_jmeter_test.sh
```

## 📊 Monitoring

- Prometheus configuration: `prometheus.yml`
- Monitoring service available on port 8090
- Health checks available on all services

## 🔐 Security

- All services run with non-root users in containers
- Database credentials are configurable via environment variables
- Redis is configured for internal network access only

## 🚀 Production Deployment

For production deployment:

1. Update environment variables in `docker-compose.yml`
2. Configure external database and Redis if needed
3. Set up proper SSL/TLS certificates
4. Configure monitoring and logging
5. Set up backup strategies

## 📝 Notes

- All Java services use OpenJDK 21
- Tests are skipped during build for faster deployment
- Services are configured for horizontal scaling
- Database migrations are handled automatically

---

**Status**: ✅ All services successfully compiled and ready for deployment!

