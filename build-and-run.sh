#!/bin/bash

# CodeBridge Platform Build and Run Script
# This script builds all services and starts them with Docker Compose

set -e

echo "🚀 CodeBridge Platform Build and Run Script"
echo "============================================"

# Initialize SDKMAN to use Java 21
source ~/.sdkman/bin/sdkman-init.sh

# Check if Java 21 is available
echo "📋 Checking Java version..."
java -version

# Check if Maven is available
echo "📋 Checking Maven..."
mvn -version

# Build all services
echo "🔨 Building all CodeBridge services..."
mvn clean package -DskipTests -Dmaven.test.skip=true \
  -pl codebridge-common,codebridge-core,codebridge-security,codebridge-gateway-service,codebridge-gitlab-service,codebridge-docker-service,codebridge-documentation-service,codebridge-server-service,codebridge-teams-service

echo "✅ All services built successfully!"

# List built JAR files
echo "📦 Built JAR files:"
find . -name "*.jar" -path "*/target/*" -not -name "*-sources.jar" -not -name "*-javadoc.jar" -not -name "*.original"

echo ""
echo "🐳 To start the services with Docker Compose, run:"
echo "   docker-compose up -d"
echo ""
echo "🌐 Service URLs (after starting with Docker Compose):"
echo "   Gateway Service:       http://localhost:8080"
echo "   Docker Service:        http://localhost:8082"
echo "   Session Service:       http://localhost:8083"
echo "   DB Service:            http://localhost:8084"
echo "   AI Service:            http://localhost:8085"
echo "   GitLab Service:        http://localhost:8086"
echo "   Documentation Service: http://localhost:8087"
echo "   Server Service:        http://localhost:8088"
echo "   Teams Service:         http://localhost:8089"
echo ""
echo "🗄️  Database URLs:"
echo "   PostgreSQL:            localhost:5432 (user: codebridge, password: codebridge, db: codebridge)"
echo "   Redis:                 localhost:6379"
echo ""
echo "✨ Build completed successfully!"
