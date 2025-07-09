#!/bin/bash

# Force rebuild script - ensures you get the latest code
echo "🔄 Force rebuilding all services with latest code..."

# Stop all containers
docker-compose -f docker-compose.local.yml down

# Remove all images to force rebuild
docker-compose -f docker-compose.local.yml down --rmi all --volumes --remove-orphans

# Build and start with no cache
docker-compose -f docker-compose.local.yml up --build --force-recreate --no-deps

echo "✅ All services rebuilt with latest code!"
