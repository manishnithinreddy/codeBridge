#!/bin/bash

# CodeBridge Services Build Script
# This script builds all Java services with Java 21

set -e  # Exit on any error

echo "🚀 Starting CodeBridge Services Build Process..."
echo "=================================================="

# Set Java 21 environment
export JAVA_HOME=/tmp/manishnithinreddy/codeBridge/jdk-21.0.7
export PATH=$JAVA_HOME/bin:$PATH

# Verify Java version
echo "☕ Java Version:"
java -version

echo ""
echo "📦 Building Services..."
echo "======================"

# List of services to build
services=(
    "codebridge-gateway-service"
    "codebridge-server-service"
    "codebridge-teams-service"
    "codebridge-monitoring-service"
    "codebridge-documentation-service"
    "codebridge-api-test-service"
    "codebridge-docker-service"
)

# Build each service
for service in "${services[@]}"; do
    echo ""
    echo "🔨 Building $service..."
    echo "------------------------"
    
    if [ -d "$service" ]; then
        cd "$service"
        
        # Clean and build
        echo "  📋 Cleaning previous build..."
        mvn clean -q
        
        echo "  🔧 Compiling sources..."
        mvn compile -DskipTests -q
        
        echo "  📦 Packaging JAR..."
        mvn package -DskipTests -q
        
        echo "  ✅ $service built successfully!"
        
        # Check if JAR was created
        if [ -f "target/$service-*.jar" ] || [ -f "target/*-SNAPSHOT.jar" ]; then
            echo "  📄 JAR file created in target/ directory"
        else
            echo "  ⚠️  Warning: JAR file not found in target/ directory"
        fi
        
        cd ..
    else
        echo "  ❌ Directory $service not found!"
        exit 1
    fi
done

echo ""
echo "🎉 All Services Built Successfully!"
echo "=================================="

# Summary
echo ""
echo "📊 Build Summary:"
echo "=================="
for service in "${services[@]}"; do
    if [ -d "$service/target" ]; then
        jar_count=$(find "$service/target" -name "*.jar" -type f | wc -l)
        echo "  ✅ $service: $jar_count JAR file(s) created"
    else
        echo "  ❌ $service: No target directory found"
    fi
done

echo ""
echo "🐳 Docker Build Instructions:"
echo "============================="
echo "To build Docker images for all services, run:"
echo "  docker-compose -f docker-compose-external.yml build"
echo ""
echo "To start all services with external configuration:"
echo "  docker-compose -f docker-compose-external.yml up -d"
echo ""
echo "To view logs:"
echo "  docker-compose -f docker-compose-external.yml logs -f [service-name]"
echo ""

echo "🎯 Next Steps:"
echo "=============="
echo "1. Review the external configuration files (application-external.yml) in each service"
echo "2. Update database connection strings if needed (currently set to 223.187.54.126:5432)"
echo "3. Set environment variables for production secrets"
echo "4. Run the Docker Compose setup"
echo ""

echo "✨ Build process completed successfully!"
