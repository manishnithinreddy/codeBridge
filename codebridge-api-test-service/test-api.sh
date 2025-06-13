#!/bin/bash

# Test script for the API Test Service
echo "Testing API Test Service..."

# Base URL
BASE_URL="http://localhost:8084"

# Test 1: Health check
echo "Test 1: Health check"
curl -s $BASE_URL/actuator/health | jq

# Test 2: Create a project
echo "Test 2: Create a project"
PROJECT_RESPONSE=$(curl -s -X POST \
  $BASE_URL/api/projects \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Project",
    "description": "A test project for API testing"
  }')
echo $PROJECT_RESPONSE | jq
PROJECT_ID=$(echo $PROJECT_RESPONSE | jq -r '.id')

# Test 3: Create a collection
echo "Test 3: Create a collection"
COLLECTION_RESPONSE=$(curl -s -X POST \
  $BASE_URL/api/projects/$PROJECT_ID/collections \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Collection",
    "description": "A test collection for API testing"
  }')
echo $COLLECTION_RESPONSE | jq
COLLECTION_ID=$(echo $COLLECTION_RESPONSE | jq -r '.id')

# Test 4: Create an API test
echo "Test 4: Create an API test"
TEST_RESPONSE=$(curl -s -X POST \
  $BASE_URL/api/collections/$COLLECTION_ID/tests \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test GET Request",
    "description": "A test GET request",
    "method": "GET",
    "url": "https://jsonplaceholder.typicode.com/posts/1",
    "headers": {
      "Accept": "application/json"
    },
    "body": null,
    "assertions": [
      {
        "type": "STATUS_CODE",
        "expected": "200"
      }
    ]
  }')
echo $TEST_RESPONSE | jq
TEST_ID=$(echo $TEST_RESPONSE | jq -r '.id')

# Test 5: Run the API test
echo "Test 5: Run the API test"
RUN_RESPONSE=$(curl -s -X POST \
  $BASE_URL/api/tests/$TEST_ID/run \
  -H "Content-Type: application/json")
echo $RUN_RESPONSE | jq

# Test 6: Get test results
echo "Test 6: Get test results"
curl -s $BASE_URL/api/tests/$TEST_ID/results | jq

echo "Tests completed!"

