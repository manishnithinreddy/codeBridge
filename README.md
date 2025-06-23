# CodeBridge

CodeBridge is a comprehensive microservices platform for managing and deploying applications across multiple environments.

## Services

The platform consists of the following microservices:

- **API Test Service**: Testing and validation service for APIs
- **Gateway Service**: API Gateway for routing requests to appropriate services
- **Docker Service**: Container management service
- **Server Service**: Server provisioning and management
- **Organization Service**: User and team management
- **Identity Service**: Authentication and authorization
- **Events Service**: Event logging and monitoring
- **GitLab Service**: GitLab integration

## Java 21 Compatibility

All services have been updated to be compatible with Java 21. The following changes were made:

1. Updated parent POM references to use `com.codebridge:codebridge-parent:0.1.0-SNAPSHOT`
2. Added Spring Cloud dependencies for service discovery
3. Standardized dependency management across all services

## Local Development Setup

### Prerequisites

- Java 21 JDK
- Maven 3.8+
- Docker and Docker Compose
- PostgreSQL (or use the provided Docker Compose setup)

### Building the Project

To build all services:

```bash
mvn clean install
```

### Running Services Locally

1. Start the discovery service first:

```bash
cd codebridge-discovery
mvn spring-boot:run
```

2. Start other services as needed:

```bash
cd codebridge-[service-name]
mvn spring-boot:run
```

## Service Dependencies

- All services depend on the discovery service for service registration
- Most services require a PostgreSQL database
- The Gateway service routes requests to other services
- The Identity service is required for authentication

## Configuration

Each service has its own application.yml file with configuration options. Common configurations include:

- Database connection settings
- Service discovery settings
- Security settings
- Logging configuration

## Contributing

1. Create a feature branch
2. Make your changes
3. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

