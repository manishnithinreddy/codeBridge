#!/bin/bash

# CodeBridge - Build and Run Script
# This script ensures you always get the latest code when running Docker Compose

echo "🚀 CodeBridge - Building and Running All Services"
echo "=================================================="

# Function to print colored output
print_status() {
    echo -e "\033[1;34m$1\033[0m"
}

print_success() {
    echo -e "\033[1;32m$1\033[0m"
}

print_error() {
    echo -e "\033[1;31m$1\033[0m"
}

# Step 1: Stop any running containers
print_status "🛑 Stopping any running containers..."
docker-compose -f docker-compose.local.yml down

# Step 2: Remove old images to force rebuild
print_status "🧹 Cleaning up old Docker images..."
docker-compose -f docker-compose.local.yml down --rmi all --volumes --remove-orphans 2>/dev/null || true

# Step 3: Build all Java services first (to ensure latest code)
print_status "🔨 Building Java services with Maven..."

services=("codebridge-gateway-service" "codebridge-security" "codebridge-teams-service" "codebridge-server-service" "codebridge-docker-service" "codebridge-gitlab-service" "codebridge-documentation-service" "codebridge-monitoring-service")

for service in "${services[@]}"; do
    if [ -d "$service" ]; then
        print_status "  📦 Building $service..."
        cd "$service"
        mvn clean package -DskipTests -q
        if [ $? -eq 0 ]; then
            print_success "    ✅ $service built successfully"
        else
            print_error "    ❌ Failed to build $service"
            cd ..
            exit 1
        fi
        cd ..
    else
        echo "    ⚠️  $service directory not found, skipping..."
    fi
done

# Step 4: Build and start all services with Docker Compose
print_status "🐳 Building and starting all services with Docker Compose..."
docker-compose -f docker-compose.local.yml up --build --force-recreate -d

# Step 5: Wait for services to start
print_status "⏳ Waiting for services to start..."
sleep 30

# Step 6: Check service health
print_status "🏥 Checking service health..."

services_health=(
    "Gateway:http://localhost:8080/actuator/health"
    "Security:http://localhost:8083/actuator/health"
    "Teams:http://localhost:8082/teams/actuator/health"
    "Server:http://localhost:8081/api/server/actuator/health"
    "Docker:http://localhost:8084/actuator/health"
    "GitLab:http://localhost:8085/actuator/health"
    "Documentation:http://localhost:8086/actuator/health"
    "Monitoring:http://localhost:8087/actuator/health"
    "Session:http://localhost:8088/health"
    "DB:http://localhost:8089/health"
    "AI:http://localhost:8090/health"
)

for service_health in "${services_health[@]}"; do
    IFS=':' read -r service_name health_url <<< "$service_health"
    
    # Try to check health (with timeout)
    if curl -f -s --max-time 5 "$health_url" > /dev/null 2>&1; then
        print_success "  ✅ $service_name service is healthy"
    else
        echo "  ⚠️  $service_name service is not responding (may still be starting)"
    fi
done

# Step 7: Show service URLs
print_success ""
print_success "🎉 All services are starting up!"
print_success "================================"
print_success ""
print_success "📋 Service URLs:"
print_success "  🌐 Gateway (Main Entry):     http://localhost:8080"
print_success "  🔐 Security Service:         http://localhost:8083"
print_success "  👥 Teams Service:            http://localhost:8082/teams"
print_success "  🖥️  Server Service:           http://localhost:8081/api/server"
print_success "  🐳 Docker Service:           http://localhost:8084"
print_success "  🦊 GitLab Service:           http://localhost:8085"
print_success "  📚 Documentation Service:    http://localhost:8086"
print_success "  📊 Monitoring Service:       http://localhost:8087"
print_success "  🔑 Session Service (Go):     http://localhost:8088"
print_success "  🗄️  DB Service (Go):          http://localhost:8089"
print_success "  🤖 AI Service (Python):      http://localhost:8090"
print_success ""
print_success "🗄️  H2 Database Consoles:"
print_success "  🔐 Security DB:              http://localhost:8083/h2-console"
print_success "  👥 Teams DB:                 http://localhost:8082/teams/h2-console"
print_success "  🖥️  Server DB:                http://localhost:8081/api/server/h2-console"
print_success "  🐳 Docker DB:                http://localhost:8084/h2-console"
print_success "  🦊 GitLab DB:                http://localhost:8085/h2-console"
print_success "  📚 Documentation DB:         http://localhost:8086/h2-console"
print_success "  📊 Monitoring DB:            http://localhost:8087/h2-console"
print_success ""
print_success "📝 To view logs: docker-compose -f docker-compose.local.yml logs -f [service-name]"
print_success "🛑 To stop all:  docker-compose -f docker-compose.local.yml down"
print_success ""
print_success "✨ Happy coding! ✨"
