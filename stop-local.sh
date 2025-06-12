#!/bin/bash

# Stop all CodeBridge services running locally

echo "Stopping CodeBridge services..."

# Find and kill Java processes for each service
pkill -f "codebridge-identity-platform"
pkill -f "codebridge-teams-service"
pkill -f "codebridge-platform-ops"

echo "All services stopped."

