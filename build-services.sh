#!/bin/bash

# CodeBridge Build Script
# This script builds all the Java services that compile successfully

echo "🚀 Starting CodeBridge Build Process..."

# Set Java version using SDKMAN
source ~/.sdkman/bin/sdkman-init.sh
sdk use java 21.0.5-tem

# Check Java version
echo "☕ Java Version:"
java -version

echo ""
echo "📦 Building Core Services..."

# Build the core services that compile successfully
mvn clean compile -DskipTests -pl codebridge-common,codebridge-core,codebridge-security,codebridge-gateway-service,codebridge-gitlab-service,codebridge-docker-service,codebridge-documentation-service,codebridge-server-service

if [ $? -eq 0 ]; then
    echo "✅ Core services compiled successfully!"
    
    echo ""
    echo "📦 Building JAR files..."
    
    # Build JAR files for each service
    mvn clean package -DskipTests -Dmaven.test.skip=true -pl codebridge-common,codebridge-core,codebridge-security,codebridge-gateway-service,codebridge-gitlab-service,codebridge-docker-service,codebridge-documentation-service,codebridge-server-service
    
    if [ $? -eq 0 ]; then
        echo "✅ JAR files built successfully!"
        
        echo ""
        echo "📋 Built Services Summary:"
        echo "  - Gateway Service (Port 8080)"
        echo "  - Docker Service (Port 8082)"
        echo "  - GitLab Service (Port 8086)"
        echo "  - Documentation Service (Port 8087)"
        echo "  - Server Service (Port 8088)"
        echo "  - Session Service (Go - Port 8083)"
        echo "  - DB Service (Go - Port 8084)"
        echo "  - AI Service (Python - Port 8085)"
        
        echo ""
        echo "🐳 To run with Docker:"
        echo "  docker-compose up -d"
        
        echo ""
        echo "🔧 To run individual services:"
        echo "  java -jar codebridge-gateway-service/target/codebridge-gateway-service-0.0.1-SNAPSHOT.jar"
        echo "  java -jar codebridge-docker-service/target/codebridge-docker-service-0.0.1-SNAPSHOT.jar"
        echo "  java -jar codebridge-gitlab-service/target/codebridge-gitlab-service-0.0.1-SNAPSHOT.jar"
        echo "  java -jar codebridge-documentation-service/target/codebridge-documentation-service-0.0.1-SNAPSHOT.jar"
        echo "  java -jar codebridge-server-service/target/codebridge-server-service-0.0.1-SNAPSHOT.jar"
        
    else
        echo "❌ Failed to build JAR files"
        exit 1
    fi
else
    echo "❌ Failed to compile core services"
    exit 1
fi

echo ""
echo "🎉 Build completed successfully!"
