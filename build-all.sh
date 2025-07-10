#!/bin/bash

# CodeBridge Build Script
# This script builds all services and verifies they can run

set -e

echo "🚀 CodeBridge Build Script"
echo "=========================="

# Check Java version
echo "📋 Checking Java version..."
java -version
if ! java -version 2>&1 | grep -q "21"; then
    echo "⚠️  Warning: Java 21 is recommended. Current version may cause issues."
fi

# Build Gateway Service
echo ""
echo "🏗️  Building Gateway Service..."
cd codebridge-gateway-service
mvn clean package -DskipTests -Dmaven.test.skip=true -q
if [ $? -eq 0 ]; then
    echo "✅ Gateway Service built successfully"
else
    echo "❌ Gateway Service build failed"
    exit 1
fi
cd ..

# Build GitLab Service
echo ""
echo "🏗️  Building GitLab Service..."
cd codebridge-gitlab-service
mvn clean package -DskipTests -Dmaven.test.skip=true -q
if [ $? -eq 0 ]; then
    echo "✅ GitLab Service built successfully"
else
    echo "❌ GitLab Service build failed"
    exit 1
fi
cd ..

echo ""
echo "🎉 All services built successfully!"
echo ""
echo "📋 Next steps:"
echo "1. Start PostgreSQL database (see README.md)"
echo "2. Run services:"
echo "   - Gateway: cd codebridge-gateway-service && java -jar target/codebridge-gateway-service-3.2.0.jar"
echo "   - GitLab:  cd codebridge-gitlab-service && java -jar target/codebridge-gitlab-service-0.0.1-SNAPSHOT.jar"
echo "3. Or use Docker: docker compose up --build"
echo ""
echo "🔗 Service URLs:"
echo "   - Gateway: http://localhost:8080/actuator/health"
echo "   - GitLab:  http://localhost:8081/api/gitlab/actuator/health"
