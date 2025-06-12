#!/bin/bash

# Start all CodeBridge services locally

echo "Starting CodeBridge services..."

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "Java is not installed. Please install Java 17 or higher."
    exit 1
fi

# Check Java version
java_version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
echo "Using Java version: $java_version"

# Create logs directory
mkdir -p logs

# Start Identity Platform
echo "Starting Identity Platform..."
cd codebridge-identity-platform
./mvnw spring-boot:run > ../logs/identity-platform.log 2>&1 &
cd ..
echo "Identity Platform started. Logs in logs/identity-platform.log"

# Wait for Identity Platform to start
echo "Waiting for Identity Platform to start..."
sleep 30

# Start Teams Service
echo "Starting Teams Service..."
cd codebridge-teams-service
./mvnw spring-boot:run > ../logs/teams-service.log 2>&1 &
cd ..
echo "Teams Service started. Logs in logs/teams-service.log"

# Wait for Teams Service to start
echo "Waiting for Teams Service to start..."
sleep 15

# Start Platform Operations
echo "Starting Platform Operations..."
cd codebridge-platform-ops
./mvnw spring-boot:run > ../logs/platform-ops.log 2>&1 &
cd ..
echo "Platform Operations started. Logs in logs/platform-ops.log"

echo "All services started. Use the following URLs to access the services:"
echo "Identity Platform: http://localhost:8081/identity"
echo "Teams Service: http://localhost:8082/teams"
echo "Platform Operations: http://localhost:8083/ops"
echo "Swagger UI for Identity Platform: http://localhost:8081/identity/swagger-ui.html"
echo "Swagger UI for Teams Service: http://localhost:8082/teams/swagger-ui.html"
echo "Swagger UI for Platform Operations: http://localhost:8083/ops/swagger-ui.html"

echo "To stop all services, run: ./stop-local.sh"

