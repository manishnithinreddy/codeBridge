#!/usr/bin/env python3
import json
import re
import time
import uuid
from urllib.parse import urlencode
import requests

class ApiTestDemo:
    """
    A demonstration of the API Test Service features using Python.
    This script shows how the key features of the API Test Service work.
    """
    
    def __init__(self):
        self.environment_variables = {
            "baseUrl": "https://jsonplaceholder.typicode.com",
            "postsPath": "posts",
            "usersPath": "users",
            "limit": "3"
        }
        self.chain_variables = {}
    
    def run_demo(self):
        """Run all the demo tests."""
        print("=== API Test Service Feature Demo ===\n")
        
        # Test 1: Direct Request Testing (GET)
        self.test_direct_request_get()
        
        # Test 2: Direct Request Testing (POST)
        self.test_direct_request_post()
        
        # Test 3: Environment Variable Substitution
        self.test_environment_variable_substitution()
        
        # Test 4: Token Injection
        self.test_token_injection()
        
        # Test 5: Chained Requests
        self.test_chained_requests()
        
        # Test 6: Snapshot Testing
        self.test_snapshot_testing()
        
        print("\n=== All Tests Completed Successfully ===")
    
    def test_direct_request_get(self):
        """Test direct GET request functionality."""
        print("=== Test 1: Direct Request Testing (GET) ===")
        
        # Create a GET request
        url = "https://jsonplaceholder.typicode.com/users/1"
        headers = {"Accept": "application/json"}
        
        print(f"Executing GET request to: {url}")
        
        # Execute the request
        start_time = time.time()
        response = requests.get(url, headers=headers)
        end_time = time.time()
        
        # Process the response
        status_code = response.status_code
        response_body = response.text
        execution_time = (end_time - start_time) * 1000  # Convert to ms
        
        print(f"Status Code: {status_code}")
        print(f"Execution Time: {execution_time:.2f}ms")
        print(f"Response Body (truncated): {response_body[:200]}...")
        
        # Verify the response
        if status_code == 200:
            print("✅ GET Request Test Passed")
        else:
            print("❌ GET Request Test Failed")
        
        print()
    
    def test_direct_request_post(self):
        """Test direct POST request functionality."""
        print("=== Test 2: Direct Request Testing (POST) ===")
        
        # Create request body
        body = {
            "title": "Test Post",
            "body": "This is a test post body",
            "userId": 1
        }
        
        # Create a POST request
        url = "https://jsonplaceholder.typicode.com/posts"
        headers = {
            "Content-Type": "application/json",
            "Accept": "application/json"
        }
        
        print(f"Executing POST request to: {url}")
        print(f"Request Body: {json.dumps(body)}")
        
        # Execute the request
        start_time = time.time()
        response = requests.post(url, headers=headers, json=body)
        end_time = time.time()
        
        # Process the response
        status_code = response.status_code
        response_body = response.text
        execution_time = (end_time - start_time) * 1000  # Convert to ms
        
        print(f"Status Code: {status_code}")
        print(f"Execution Time: {execution_time:.2f}ms")
        print(f"Response Body: {response_body}")
        
        # Verify the response
        if status_code == 201:
            print("✅ POST Request Test Passed")
        else:
            print("❌ POST Request Test Failed")
        
        print()
    
    def test_environment_variable_substitution(self):
        """Test environment variable substitution functionality."""
        print("=== Test 3: Environment Variable Substitution ===")
        
        # Create a URL with environment variables
        url = "{{baseUrl}}/{{usersPath}}?_limit={{limit}}"
        print(f"Original URL: {url}")
        
        # Process environment variables
        processed_url = self.process_environment_variables(url)
        print(f"Processed URL: {processed_url}")
        
        # Create and execute the request
        headers = {"Accept": "application/json"}
        response = requests.get(processed_url, headers=headers)
        
        # Process the response
        status_code = response.status_code
        response_body = response.text
        
        print(f"Status Code: {status_code}")
        print(f"Response Body (truncated): {response_body[:200]}...")
        
        # Verify the response
        if status_code == 200 and "id" in response_body and "name" in response_body:
            print("✅ Environment Variable Substitution Test Passed")
        else:
            print("❌ Environment Variable Substitution Test Failed")
        
        print()
    
    def test_token_injection(self):
        """Test token injection functionality."""
        print("=== Test 4: Token Injection ===")
        
        # Simulate token data
        token_type = "Bearer"
        token_value = f"test-token-{str(uuid.uuid4())[:8]}"
        
        # Create a request that requires authentication
        url = "https://httpbin.org/headers"
        headers = {"Authorization": f"{token_type} {token_value}"}
        
        print(f"Injecting token: {token_type} {token_value}")
        
        # Execute the request
        response = requests.get(url, headers=headers)
        
        # Process the response
        status_code = response.status_code
        response_body = response.text
        
        print(f"Status Code: {status_code}")
        print(f"Response Body: {response_body}")
        
        # Verify the response contains our injected token
        if status_code == 200 and token_value in response_body:
            print("✅ Token Injection Test Passed")
        else:
            print("❌ Token Injection Test Failed")
        
        print()
    
    def test_chained_requests(self):
        """Test chained requests functionality."""
        print("=== Test 5: Chained Requests ===")
        
        # Step 1: Get a user
        print("Step 1: Get user data")
        user_url = "https://jsonplaceholder.typicode.com/users/1"
        user_response = requests.get(user_url, headers={"Accept": "application/json"})
        user_data = user_response.json()
        
        print(f"User Response: {json.dumps(user_data)}")
        
        # Extract user ID and name from response
        user_id = str(user_data["id"])
        user_name = user_data["name"]
        
        self.chain_variables["userId"] = user_id
        self.chain_variables["userName"] = user_name
        
        print("Extracted variables:")
        print(f"  userId: {user_id}")
        print(f"  userName: {user_name}")
        
        # Step 2: Create a post using the extracted user ID
        print("\nStep 2: Create a post using extracted user ID")
        
        post_body_template = {
            "title": "Post by {{userName}}",
            "body": "This is a test post created for user with ID {{userId}}",
            "userId": "{{userId}}"
        }
        
        # Process variables in the body
        post_body = {}
        for key, value in post_body_template.items():
            if isinstance(value, str):
                post_body[key] = self.process_chain_variables(value)
            else:
                post_body[key] = value
        
        # Convert userId to integer
        post_body["userId"] = int(post_body["userId"])
        
        print(f"Processed Body: {json.dumps(post_body)}")
        
        post_url = "https://jsonplaceholder.typicode.com/posts"
        post_response = requests.post(
            post_url,
            headers={"Content-Type": "application/json", "Accept": "application/json"},
            json=post_body
        )
        post_data = post_response.json()
        
        print(f"Post Response: {json.dumps(post_data)}")
        
        # Extract post ID from response
        post_id = str(post_data["id"])
        self.chain_variables["postId"] = post_id
        
        print(f"Extracted postId: {post_id}")
        
        # Step 3: Get comments for the post
        print("\nStep 3: Get comments for the post")
        
        comments_url_template = "https://jsonplaceholder.typicode.com/posts/{{postId}}/comments"
        comments_url = self.process_chain_variables(comments_url_template)
        
        print(f"Processed URL: {comments_url}")
        
        comments_response = requests.get(comments_url, headers={"Accept": "application/json"})
        comments_data = comments_response.json()
        
        print(f"Comments Response (truncated): {json.dumps(comments_data[:2])}...")
        
        # Verify the chain worked correctly
        if comments_response.status_code == 200 and len(comments_data) > 0 and "email" in comments_data[0]:
            print("✅ Chained Requests Test Passed")
        else:
            print("❌ Chained Requests Test Failed")
        
        print()
    
    def test_snapshot_testing(self):
        """Test snapshot testing functionality."""
        print("=== Test 6: Snapshot Testing ===")
        
        # Step 1: Create a snapshot (get a user)
        print("Step 1: Create a snapshot")
        user_url = "https://jsonplaceholder.typicode.com/users/1"
        user_response = requests.get(user_url, headers={"Accept": "application/json"})
        user_data = user_response.json()
        
        # Save the snapshot
        snapshot = user_data
        print(f"Created snapshot: {json.dumps(snapshot)}")
        
        # Step 2: Compare with matching response
        print("\nStep 2: Compare with matching response")
        same_user_response = requests.get(user_url, headers={"Accept": "application/json"})
        same_user_data = same_user_response.json()
        
        # Compare responses
        differences = self.compare_json(snapshot, same_user_data)
        
        if not differences:
            print("✅ Response matches snapshot")
        else:
            print("❌ Response differs from snapshot")
            print("Differences:")
            for path, diff in differences.items():
                print(f"  {path}: {diff}")
        
        # Step 3: Compare with modified response
        print("\nStep 3: Compare with modified response")
        modified_data = same_user_data.copy()
        modified_data["name"] = "Modified Name"
        modified_data["email"] = "modified@example.com"
        
        # Compare responses
        differences = self.compare_json(snapshot, modified_data)
        
        if not differences:
            print("✅ Response matches snapshot")
        else:
            print("❌ Response differs from snapshot")
            print("Differences:")
            for path, diff in differences.items():
                print(f"  {path}: {diff}")
        
        print()
    
    def process_environment_variables(self, input_str):
        """Process environment variables in a string."""
        if not input_str or not self.environment_variables:
            return input_str
        
        result = input_str
        for key, value in self.environment_variables.items():
            result = result.replace(f"{{{{{key}}}}}", value)
        
        return result
    
    def process_chain_variables(self, input_str):
        """Process chain variables in a string."""
        if not input_str or not self.chain_variables:
            return input_str
        
        result = input_str
        for key, value in self.chain_variables.items():
            result = result.replace(f"{{{{{key}}}}}", value)
        
        return result
    
    def compare_json(self, expected, actual, path="$"):
        """Compare two JSON objects and return differences."""
        differences = {}
        
        if expected == actual:
            return differences
        
        if type(expected) != type(actual):
            differences[path] = f"Type mismatch: expected {type(expected)}, got {type(actual)}"
            return differences
        
        if isinstance(expected, dict):
            # Compare dictionaries
            for key in expected:
                key_path = f"{path}.{key}"
                if key in actual:
                    nested_diffs = self.compare_json(expected[key], actual[key], key_path)
                    differences.update(nested_diffs)
                else:
                    differences[key_path] = "Field missing in actual"
            
            # Check for extra fields in actual
            for key in actual:
                if key not in expected:
                    differences[f"{path}.{key}"] = "Extra field in actual"
        
        elif isinstance(expected, list):
            # Compare lists
            if len(expected) != len(actual):
                differences[path] = f"Array size mismatch: expected {len(expected)}, got {len(actual)}"
                return differences
            
            for i in range(len(expected)):
                item_path = f"{path}[{i}]"
                nested_diffs = self.compare_json(expected[i], actual[i], item_path)
                differences.update(nested_diffs)
        
        else:
            # Compare primitive values
            differences[path] = f"Value mismatch: expected {expected}, got {actual}"
        
        return differences


if __name__ == "__main__":
    demo = ApiTestDemo()
    demo.run_demo()

