#!/bin/bash

echo "🧪 Testing Docker Compose Setup"
echo "==============================="

# Test 1: Validate Docker Compose file
echo "📋 Test 1: Validating docker-compose.local.yml..."
if docker compose -f docker-compose.local.yml config --quiet; then
    echo "✅ Docker Compose file is valid"
else
    echo "❌ Docker Compose file has errors"
    exit 1
fi

# Test 2: Check if all services are defined
echo ""
echo "📋 Test 2: Checking service definitions..."
services=$(docker compose -f docker-compose.local.yml config --services)
expected_services=(
    "codebridge-gateway"
    "codebridge-security" 
    "codebridge-teams"
    "codebridge-server"
    "codebridge-docker"
    "codebridge-gitlab"
    "codebridge-documentation"
    "codebridge-monitoring"
    "codebridge-session"
    "codebridge-db"
    "codebridge-ai"
)

echo "Found services:"
echo "$services"
echo ""

for service in "${expected_services[@]}"; do
    if echo "$services" | grep -q "^$service$"; then
        echo "✅ $service service is defined"
    else
        echo "❌ $service service is missing"
    fi
done

# Test 3: Check if JAR files exist for Java services
echo ""
echo "📋 Test 3: Checking if JAR files exist..."
java_services=("codebridge-gateway-service" "codebridge-security" "codebridge-teams-service" "codebridge-server-service" "codebridge-docker-service" "codebridge-gitlab-service" "codebridge-documentation-service" "codebridge-monitoring-service")

for service in "${java_services[@]}"; do
    jar_file=$(find "$service/target" -name "*.jar" -not -name "*-sources.jar" -not -name "*-javadoc.jar" 2>/dev/null | head -1)
    if [ -n "$jar_file" ]; then
        echo "✅ $service JAR file exists: $(basename "$jar_file")"
    else
        echo "❌ $service JAR file not found"
    fi
done

# Test 4: Check port conflicts
echo ""
echo "📋 Test 4: Checking for port conflicts..."
ports=(8080 8081 8082 8083 8084 8085 8086 8087 8088 8089 8090)

for port in "${ports[@]}"; do
    if netstat -tuln 2>/dev/null | grep -q ":$port "; then
        echo "⚠️  Port $port is already in use"
    else
        echo "✅ Port $port is available"
    fi
done

echo ""
echo "🎉 Docker setup test completed!"
echo ""
echo "📝 Next steps:"
echo "   1. Run: ./build-and-run.sh"
echo "   2. Or run: docker compose -f docker-compose.local.yml up --build"
echo "   3. Access services at http://localhost:8080 (Gateway)"
