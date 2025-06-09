#!/usr/bin/env python3

import requests
import json
import time
import sys
import uuid

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
    try:
        health_response = requests.get(f"{BASE_URL}/actuator/health")
        print_response(health_response, "Health Check")
    except Exception as e:
        print(f"Health check failed: {e}")
        print("Service might still be starting up, continuing with tests...")
    
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
    print(f"Project created with ID: {project_id}")
    
    # Test 3: Get all projects
    projects_response = requests.get(f"{BASE_URL}/api/projects")
    projects = print_response(projects_response, "Get All Projects")
    if not projects:
        print("Failed to get projects. Exiting.")
        sys.exit(1)
    
    # Test 4: Get project by ID
    project_by_id_response = requests.get(f"{BASE_URL}/api/projects/{project_id}")
    project_by_id = print_response(project_by_id_response, "Get Project by ID")
    if not project_by_id:
        print("Failed to get project by ID. Exiting.")
        sys.exit(1)
    
    # Test 5: Create a collection
    collection_data = {
        "name": "Python Test Collection",
        "projectId": project_id
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
    print(f"Collection created with ID: {collection_id}")
    
    # Test 6: Get all collections for project
    collections_response = requests.get(f"{BASE_URL}/api/projects/{project_id}/collections")
    collections = print_response(collections_response, "Get All Collections for Project")
    if not collections:
        print("Failed to get collections. Exiting.")
        sys.exit(1)
    
    # Test 7: Get collection by ID
    collection_by_id_response = requests.get(f"{BASE_URL}/api/projects/{project_id}/collections/{collection_id}")
    collection_by_id = print_response(collection_by_id_response, "Get Collection by ID")
    if not collection_by_id:
        print("Failed to get collection by ID. Exiting.")
        sys.exit(1)
    
    # Test 8: Create an API test
    test_data = {
        "name": "Test GET Request to HTTPBin",
        "description": "A test GET request to HTTPBin",
        "method": "GET",
        "url": "https://httpbin.org/get",
        "timeoutMs": 5000,
        "protocolType": "HTTP"
    }
    test_response = requests.post(
        f"{BASE_URL}/api/tests",
        json=test_data
    )
    test = print_response(test_response, "Create API Test")
    if not test:
        print("Failed to create API test. Exiting.")
        sys.exit(1)
    
    test_id = test["id"]
    print(f"API Test created with ID: {test_id}")
    
    # Test 9: Get all tests
    tests_response = requests.get(f"{BASE_URL}/api/tests")
    tests = print_response(tests_response, "Get All Tests")
    if not tests:
        print("Failed to get tests. Exiting.")
        sys.exit(1)
    
    # Test 10: Get test by ID
    test_by_id_response = requests.get(f"{BASE_URL}/api/tests/{test_id}")
    test_by_id = print_response(test_by_id_response, "Get Test by ID")
    if not test_by_id:
        print("Failed to get test by ID. Exiting.")
        sys.exit(1)
    
    # Test 11: Run the API test
    run_response = requests.post(
        f"{BASE_URL}/api/tests/{test_id}/run"
    )
    run_result = print_response(run_response, "Run API Test")
    if not run_result:
        print("Failed to run API test. Exiting.")
        sys.exit(1)
    
    # Test 12: Get test results
    results_response = requests.get(f"{BASE_URL}/api/tests/{test_id}/results")
    results = print_response(results_response, "Get Test Results")
    
    # Test 13: Create a POST test
    post_test_data = {
        "name": "Test POST Request to HTTPBin",
        "description": "A test POST request to HTTPBin",
        "method": "POST",
        "url": "https://httpbin.org/post",
        "timeoutMs": 5000,
        "protocolType": "HTTP",
        "requestBody": json.dumps({"message": "Hello from Python test"})
    }
    post_test_response = requests.post(
        f"{BASE_URL}/api/tests",
        json=post_test_data
    )
    post_test = print_response(post_test_response, "Create POST API Test")
    if not post_test:
        print("Failed to create POST API test. Exiting.")
        sys.exit(1)
    
    post_test_id = post_test["id"]
    print(f"POST API Test created with ID: {post_test_id}")
    
    # Test 14: Run the POST API test
    post_run_response = requests.post(
        f"{BASE_URL}/api/tests/{post_test_id}/run"
    )
    post_run_result = print_response(post_run_response, "Run POST API Test")
    if not post_run_result:
        print("Failed to run POST API test. Exiting.")
        sys.exit(1)
    
    # Test 15: Update an API test
    update_test_data = {
        "name": "Updated Test GET Request",
        "description": "An updated test GET request",
        "method": "GET",
        "url": "https://httpbin.org/get?param=updated",
        "timeoutMs": 6000,
        "protocolType": "HTTP"
    }
    update_test_response = requests.put(
        f"{BASE_URL}/api/tests/{test_id}",
        json=update_test_data
    )
    updated_test = print_response(update_test_response, "Update API Test")
    if not updated_test:
        print("Failed to update API test. Exiting.")
        sys.exit(1)
    
    # Test 16: Run the updated API test
    updated_run_response = requests.post(
        f"{BASE_URL}/api/tests/{test_id}/run"
    )
    updated_run_result = print_response(updated_run_response, "Run Updated API Test")
    if not updated_run_result:
        print("Failed to run updated API test. Exiting.")
        sys.exit(1)
    
    # Test 17: Update collection
    update_collection_data = {
        "name": "Updated Python Test Collection",
        "projectId": project_id
    }
    update_collection_response = requests.put(
        f"{BASE_URL}/api/projects/{project_id}/collections/{collection_id}",
        json=update_collection_data
    )
    updated_collection = print_response(update_collection_response, "Update Collection")
    if not updated_collection:
        print("Failed to update collection. Exiting.")
        sys.exit(1)
    
    print("\nAll tests completed successfully!")
    print("\nSummary:")
    print(f"- Created project: {project['name']} (ID: {project_id})")
    print(f"- Created collection: {collection['name']} (ID: {collection_id})")
    print(f"- Created GET test: {test['name']} (ID: {test_id})")
    print(f"- Created POST test: {post_test['name']} (ID: {post_test_id})")
    print(f"- Executed tests successfully")
    print(f"- Updated test and collection successfully")
    
    # Optional: Clean up (uncomment to enable)
    """
    # Delete tests
    print("\nCleaning up...")
    requests.delete(f"{BASE_URL}/api/tests/{test_id}")
    requests.delete(f"{BASE_URL}/api/tests/{post_test_id}")
    
    # Delete collection
    requests.delete(f"{BASE_URL}/api/projects/{project_id}/collections/{collection_id}")
    
    # Delete project
    requests.delete(f"{BASE_URL}/api/projects/{project_id}")
    print("Cleanup completed.")
    """

if __name__ == "__main__":
    test_api_service()

