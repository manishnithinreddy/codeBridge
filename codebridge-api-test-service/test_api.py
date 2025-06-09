#!/usr/bin/env python3

import requests
import json
import time
import sys

# Base URL
BASE_URL = "http://localhost:8084"

def print_response(response, label):
    print(f"\n=== {label} ===")
    print(f"Status Code: {response.status_code}")
    try:
        print(json.dumps(response.json(), indent=2))
    except:
        print(response.text)
    print("=" * (len(label) + 8))
    return response.json() if response.status_code < 400 else None

def test_api_service():
    print("Testing API Test Service...")
    
    # Test 1: Health check
    health_response = requests.get(f"{BASE_URL}/actuator/health")
    print_response(health_response, "Health Check")
    
    # Test 2: Create a project
    project_data = {
        "name": "Python Test Project",
        "description": "A test project created from Python"
    }
    project_response = requests.post(
        f"{BASE_URL}/api/projects",
        json=project_data
    )
    project = print_response(project_response, "Create Project")
    if not project:
        print("Failed to create project. Exiting.")
        sys.exit(1)
    
    project_id = project["id"]
    
    # Test 3: Create a collection
    collection_data = {
        "name": "Python Test Collection",
        "description": "A test collection created from Python"
    }
    collection_response = requests.post(
        f"{BASE_URL}/api/projects/{project_id}/collections",
        json=collection_data
    )
    collection = print_response(collection_response, "Create Collection")
    if not collection:
        print("Failed to create collection. Exiting.")
        sys.exit(1)
    
    collection_id = collection["id"]
    
    # Test 4: Create an environment
    env_data = {
        "name": "Test Environment",
        "variables": {
            "base_url": "https://jsonplaceholder.typicode.com",
            "post_id": "1"
        }
    }
    env_response = requests.post(
        f"{BASE_URL}/api/projects/{project_id}/environments",
        json=env_data
    )
    env = print_response(env_response, "Create Environment")
    if not env:
        print("Failed to create environment. Exiting.")
        sys.exit(1)
    
    env_id = env["id"]
    
    # Test 5: Create an API test with environment variables
    test_data = {
        "name": "Test GET Request with Environment",
        "description": "A test GET request using environment variables",
        "method": "GET",
        "url": "{{base_url}}/posts/{{post_id}}",
        "headers": {
            "Accept": "application/json"
        },
        "body": None,
        "assertions": [
            {
                "type": "STATUS_CODE",
                "expected": "200"
            },
            {
                "type": "JSON_PATH",
                "path": "$.id",
                "expected": "1"
            }
        ],
        "environmentId": env_id
    }
    test_response = requests.post(
        f"{BASE_URL}/api/collections/{collection_id}/tests",
        json=test_data
    )
    test = print_response(test_response, "Create API Test")
    if not test:
        print("Failed to create API test. Exiting.")
        sys.exit(1)
    
    test_id = test["id"]
    
    # Test 6: Run the API test
    run_response = requests.post(
        f"{BASE_URL}/api/tests/{test_id}/run",
        json={}
    )
    run_result = print_response(run_response, "Run API Test")
    
    # Test 7: Create a snapshot
    snapshot_response = requests.post(
        f"{BASE_URL}/api/tests/{test_id}/snapshots",
        json={"name": "Baseline Snapshot"}
    )
    snapshot = print_response(snapshot_response, "Create Snapshot")
    
    # Test 8: Compare with snapshot
    compare_response = requests.post(
        f"{BASE_URL}/api/tests/{test_id}/compare",
        json={"snapshotId": snapshot["id"]}
    )
    print_response(compare_response, "Compare with Snapshot")
    
    print("\nTests completed successfully!")

if __name__ == "__main__":
    test_api_service()

