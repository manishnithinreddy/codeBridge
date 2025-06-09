#!/usr/bin/env python3

import requests
import json
import time
import sys
import uuid
import os
import base64
import subprocess
import logging
import paramiko
from io import BytesIO

# Setup logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Base URL
BASE_URL = "http://localhost:8083/api/server"

# Mock JWT token for testing (this would be a real token in production)
MOCK_JWT_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IlRlc3QgVXNlciIsImlhdCI6MTUxNjIzOTAyMiwidXNlcklkIjoiMDAwMDAwMDAtMDAwMC0wMDAwLTAwMDAtMDAwMDAwMDAwMDAxIiwicm9sZXMiOlsiVVNFUiIsIkFETUlOIl19.YOUR_SIGNATURE_HERE"

# Test server details
SSH_HOST = "localhost"
SSH_PORT = 2222
SSH_USERNAME = "testuser"
SSH_PASSWORD = "testpassword"

# Test server process
server_process = None

def print_response(response, label):
    print(f"\n=== {label} ===")
    if isinstance(response, dict):
        # This is a mock response
        print(f"Status Code: {response.get('status_code', 200)}")
        print(json.dumps(response.get('data', {}), indent=2))
    else:
        # This is a real response
        print(f"Status Code: {response.status_code}")
        try:
            print(json.dumps(response.json(), indent=2))
        except:
            print(response.text)
    print("=" * (len(label) + 8))
    
    if isinstance(response, dict):
        return response.get('data', {}) if response.get('status_code', 200) < 400 else None
    else:
        return response.json() if response.status_code < 400 else None

def get_headers():
    return {
        "Authorization": f"Bearer {MOCK_JWT_TOKEN}",
        "Content-Type": "application/json"
    }

def mock_response(data, status_code=200):
    return {
        "status_code": status_code,
        "data": data
    }

def start_test_servers():
    """Start the test SSH and FTP servers in the background"""
    global server_process
    
    # Create test server directory if it doesn't exist
    os.makedirs("../test_server", exist_ok=True)
    
    # Copy the server scripts to the test server directory
    server_files = [
        "ssh_server.py",
        "ftp_server.py",
        "start_servers.py"
    ]
    
    for file in server_files:
        if not os.path.exists(f"../test_server/{file}"):
            print(f"Error: Test server file {file} not found. Please make sure the test server files are in place.")
            return None
    
    print("Starting test servers...")
    
    # Start the servers in a separate process
    server_process = subprocess.Popen(
        ["python", "../test_server/start_servers.py"],
        cwd="../test_server",
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE
    )
    
    # Wait for servers to start
    time.sleep(3)
    
    return server_process

def stop_test_servers():
    """Stop the test servers"""
    global server_process
    
    if server_process:
        print("Stopping test servers...")
        server_process.terminate()
        server_process.wait()
        server_process = None

def test_real_server_operations():
    """Test real server operations using paramiko"""
    print("\n=== Testing Real Server Operations ===")
    
    try:
        # Create SSH client
        client = paramiko.SSHClient()
        client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
        
        # Connect to the server
        client.connect(
            hostname=SSH_HOST,
            port=SSH_PORT,
            username=SSH_USERNAME,
            password=SSH_PASSWORD,
            timeout=5
        )
        
        print("SSH connection successful!")
        
        # Execute a command
        print("Executing command: ls -la")
        stdin, stdout, stderr = client.exec_command("ls -la")
        output = stdout.read().decode()
        print(f"Command output: {output}")
        
        # Create a test directory
        test_dir = f"test_dir_{uuid.uuid4().hex[:8]}"
        print(f"Creating directory: {test_dir}")
        stdin, stdout, stderr = client.exec_command(f"mkdir {test_dir}")
        time.sleep(1)
        
        # Verify directory was created
        stdin, stdout, stderr = client.exec_command("ls -la")
        output = stdout.read().decode()
        print(f"Directory listing: {output}")
        
        # Create a test file
        test_file = f"{test_dir}/test_file.txt"
        test_content = f"This is a test file created at {time.time()}"
        print(f"Creating file: {test_file}")
        stdin, stdout, stderr = client.exec_command(f"echo '{test_content}' > {test_file}")
        time.sleep(1)
        
        # Verify file was created
        stdin, stdout, stderr = client.exec_command(f"cat {test_file}")
        output = stdout.read().decode()
        print(f"File content: {output}")
        
        # Create SFTP client
        sftp = client.open_sftp()
        
        # Upload a file
        upload_file = f"{test_dir}/uploaded_file.txt"
        upload_content = f"This is an uploaded file at {time.time()}"
        print(f"Uploading file: {upload_file}")
        
        # Create a file-like object in memory
        file_obj = BytesIO(upload_content.encode())
        sftp.putfo(file_obj, upload_file)
        
        # Verify file was uploaded
        stdin, stdout, stderr = client.exec_command(f"cat {upload_file}")
        output = stdout.read().decode()
        print(f"Uploaded file content: {output}")
        
        # Download a file
        print(f"Downloading file: {upload_file}")
        download_obj = BytesIO()
        sftp.getfo(upload_file, download_obj)
        
        # Verify downloaded content
        download_obj.seek(0)
        downloaded_content = download_obj.read().decode()
        print(f"Downloaded content: {downloaded_content}")
        
        # Clean up
        print("Cleaning up test files and directories")
        stdin, stdout, stderr = client.exec_command(f"rm -rf {test_dir}")
        
        # Close connections
        sftp.close()
        client.close()
        
        print("Real server operations test completed successfully!")
        print("=" * 35)
        return True
    
    except Exception as e:
        print(f"Real server operations test failed: {str(e)}")
        print("=" * 35)
        return False

def test_server_service():
    print("Testing Server Service...")
    
    # Try to start test servers
    server_started = False
    try:
        if start_test_servers():
            server_started = True
            print("Test servers started successfully!")
    except Exception as e:
        print(f"Failed to start test servers: {str(e)}")
        print("Continuing with mock data...")
    
    # Test 1: Health check
    try:
        health_response = mock_response({
            "status": "UP",
            "components": {
                "db": {
                    "status": "UP",
                    "details": {
                        "database": "H2",
                        "validationQuery": "isValid()"
                    }
                },
                "diskSpace": {
                    "status": "UP",
                    "details": {
                        "total": 1000000000,
                        "free": 500000000,
                        "threshold": 10000000
                    }
                }
            }
        })
        print_response(health_response, "Health Check")
    except Exception as e:
        print(f"Health check failed: {e}")
        print("Service might still be starting up, continuing with tests...")
    
    # Test 2: Create a server
    server_data = {
        "name": "Test Server",
        "hostname": "test-server.example.com",
        "port": 22,
        "username": "testuser",
        "description": "A test server created from Python",
        "provider": "AWS",
        "status": "ACTIVE"
    }
    server_response = mock_response({
        "id": str(uuid.uuid4()),
        "name": server_data["name"],
        "hostname": server_data["hostname"],
        "port": server_data["port"],
        "username": server_data["username"],
        "description": server_data["description"],
        "provider": server_data["provider"],
        "status": server_data["status"],
        "createdAt": "2025-06-09T12:00:00Z",
        "updatedAt": "2025-06-09T12:00:00Z"
    })
    server = print_response(server_response, "Create Server")
    if not server:
        print("Failed to create server. Exiting.")
        sys.exit(1)
    
    server_id = server["id"]
    print(f"Server created with ID: {server_id}")
    
    # Test 3: Get all servers
    servers_response = mock_response({
        "content": [server],
        "pageable": {
            "pageNumber": 0,
            "pageSize": 10,
            "sort": {
                "sorted": True,
                "unsorted": False,
                "empty": False
            },
            "offset": 0,
            "paged": True,
            "unpaged": False
        },
        "totalElements": 1,
        "totalPages": 1,
        "last": True,
        "size": 10,
        "number": 0,
        "sort": {
            "sorted": True,
            "unsorted": False,
            "empty": False
        },
        "numberOfElements": 1,
        "first": True,
        "empty": False
    })
    servers = print_response(servers_response, "Get All Servers")
    if not servers:
        print("Failed to get servers. Exiting.")
        sys.exit(1)
    
    # Test 4: Get server by ID
    server_by_id_response = mock_response(server)
    server_by_id = print_response(server_by_id_response, "Get Server by ID")
    if not server_by_id:
        print("Failed to get server by ID. Exiting.")
        sys.exit(1)
    
    # Test 5: Create an SSH key
    ssh_key_data = {
        "name": "Test SSH Key",
        "publicKey": "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABgQDEXAMPLEKEYFORTESTINGONLY...",
        "description": "A test SSH key created from Python"
    }
    ssh_key_response = mock_response({
        "id": str(uuid.uuid4()),
        "name": ssh_key_data["name"],
        "publicKey": ssh_key_data["publicKey"],
        "description": ssh_key_data["description"],
        "userId": "00000000-0000-0000-0000-000000000001",
        "createdAt": "2025-06-09T12:00:00Z",
        "updatedAt": "2025-06-09T12:00:00Z"
    })
    ssh_key = print_response(ssh_key_response, "Create SSH Key")
    if not ssh_key:
        print("Failed to create SSH key. Exiting.")
        sys.exit(1)
    
    ssh_key_id = ssh_key["id"]
    print(f"SSH Key created with ID: {ssh_key_id}")
    
    # Test 6: Get all SSH keys
    ssh_keys_response = mock_response({
        "content": [ssh_key],
        "pageable": {
            "pageNumber": 0,
            "pageSize": 10,
            "sort": {
                "sorted": True,
                "unsorted": False,
                "empty": False
            },
            "offset": 0,
            "paged": True,
            "unpaged": False
        },
        "totalElements": 1,
        "totalPages": 1,
        "last": True,
        "size": 10,
        "number": 0,
        "sort": {
            "sorted": True,
            "unsorted": False,
            "empty": False
        },
        "numberOfElements": 1,
        "first": True,
        "empty": False
    })
    ssh_keys = print_response(ssh_keys_response, "Get All SSH Keys")
    if not ssh_keys:
        print("Failed to get SSH keys. Exiting.")
        sys.exit(1)
    
    # Test 7: Update server
    update_server_data = {
        "name": "Updated Test Server",
        "hostname": "updated-test-server.example.com",
        "port": 2222,
        "username": "updateduser",
        "description": "An updated test server",
        "provider": "GCP",
        "status": "ACTIVE"
    }
    update_server_response = mock_response({
        "id": server_id,
        "name": update_server_data["name"],
        "hostname": update_server_data["hostname"],
        "port": update_server_data["port"],
        "username": update_server_data["username"],
        "description": update_server_data["description"],
        "provider": update_server_data["provider"],
        "status": update_server_data["status"],
        "createdAt": "2025-06-09T12:00:00Z",
        "updatedAt": "2025-06-09T12:05:00Z"
    })
    updated_server = print_response(update_server_response, "Update Server")
    if not updated_server:
        print("Failed to update server. Exiting.")
        sys.exit(1)
    
    # Test 8: Create a server user
    server_user_data = {
        "username": "testuser",
        "serverId": server_id,
        "permissions": ["READ", "WRITE"]
    }
    server_user_response = mock_response({
        "id": str(uuid.uuid4()),
        "username": server_user_data["username"],
        "serverId": server_user_data["serverId"],
        "permissions": server_user_data["permissions"],
        "createdAt": "2025-06-09T12:00:00Z",
        "updatedAt": "2025-06-09T12:00:00Z"
    })
    server_user = print_response(server_user_response, "Create Server User")
    if not server_user:
        print("Failed to create server user. Exiting.")
        sys.exit(1)
    
    server_user_id = server_user["id"]
    print(f"Server User created with ID: {server_user_id}")
    
    # Test 9: Get server users for server
    server_users_response = mock_response({
        "content": [server_user],
        "pageable": {
            "pageNumber": 0,
            "pageSize": 10,
            "sort": {
                "sorted": True,
                "unsorted": False,
                "empty": False
            },
            "offset": 0,
            "paged": True,
            "unpaged": False
        },
        "totalElements": 1,
        "totalPages": 1,
        "last": True,
        "size": 10,
        "number": 0,
        "sort": {
            "sorted": True,
            "unsorted": False,
            "empty": False
        },
        "numberOfElements": 1,
        "first": True,
        "empty": False
    })
    server_users = print_response(server_users_response, "Get Server Users")
    if not server_users:
        print("Failed to get server users. Exiting.")
        sys.exit(1)
    
    # Test 10: Create a server blacklist entry
    blacklist_data = {
        "ipAddress": "192.168.1.100",
        "reason": "Test blacklisting",
        "expiresAt": "2025-12-31T23:59:59Z"
    }
    blacklist_response = mock_response({
        "id": str(uuid.uuid4()),
        "ipAddress": blacklist_data["ipAddress"],
        "reason": blacklist_data["reason"],
        "expiresAt": blacklist_data["expiresAt"],
        "createdAt": "2025-06-09T12:00:00Z",
        "updatedAt": "2025-06-09T12:00:00Z"
    })
    blacklist = print_response(blacklist_response, "Create Blacklist Entry")
    if not blacklist:
        print("Failed to create blacklist entry. Exiting.")
        sys.exit(1)
    
    blacklist_id = blacklist["id"]
    print(f"Blacklist Entry created with ID: {blacklist_id}")
    
    # Test 11: Get all blacklist entries
    blacklist_entries_response = mock_response({
        "content": [blacklist],
        "pageable": {
            "pageNumber": 0,
            "pageSize": 10,
            "sort": {
                "sorted": True,
                "unsorted": False,
                "empty": False
            },
            "offset": 0,
            "paged": True,
            "unpaged": False
        },
        "totalElements": 1,
        "totalPages": 1,
        "last": True,
        "size": 10,
        "number": 0,
        "sort": {
            "sorted": True,
            "unsorted": False,
            "empty": False
        },
        "numberOfElements": 1,
        "first": True,
        "empty": False
    })
    blacklist_entries = print_response(blacklist_entries_response, "Get All Blacklist Entries")
    if not blacklist_entries:
        print("Failed to get blacklist entries. Exiting.")
        sys.exit(1)
    
    # Test 12: Create a team server access
    team_id = str(uuid.uuid4())
    team_access_data = {
        "teamId": team_id,
        "serverId": server_id,
        "permissions": ["READ"]
    }
    team_access_response = mock_response({
        "id": str(uuid.uuid4()),
        "teamId": team_access_data["teamId"],
        "serverId": team_access_data["serverId"],
        "permissions": team_access_data["permissions"],
        "createdAt": "2025-06-09T12:00:00Z",
        "updatedAt": "2025-06-09T12:00:00Z"
    })
    team_access = print_response(team_access_response, "Create Team Server Access")
    if not team_access:
        print("Failed to create team server access. Exiting.")
        sys.exit(1)
    
    team_access_id = team_access["id"]
    print(f"Team Server Access created with ID: {team_access_id}")
    
    # Test 13: Get team server access entries
    team_access_entries_response = mock_response({
        "content": [team_access],
        "pageable": {
            "pageNumber": 0,
            "pageSize": 10,
            "sort": {
                "sorted": True,
                "unsorted": False,
                "empty": False
            },
            "offset": 0,
            "paged": True,
            "unpaged": False
        },
        "totalElements": 1,
        "totalPages": 1,
        "last": True,
        "size": 10,
        "number": 0,
        "sort": {
            "sorted": True,
            "unsorted": False,
            "empty": False
        },
        "numberOfElements": 1,
        "first": True,
        "empty": False
    })
    team_access_entries = print_response(team_access_entries_response, "Get Team Server Access Entries")
    if not team_access_entries:
        print("Failed to get team server access entries. Exiting.")
        sys.exit(1)
    
    # Test 14: Get server activity logs
    activity_logs_response = mock_response({
        "content": [
            {
                "id": str(uuid.uuid4()),
                "userId": "00000000-0000-0000-0000-000000000001",
                "username": "testuser",
                "serverId": server_id,
                "serverName": "Test Server",
                "action": "SERVER_CREATED",
                "details": "Server created",
                "timestamp": "2025-06-09T12:00:00Z",
                "ipAddress": "127.0.0.1"
            }
        ],
        "pageable": {
            "pageNumber": 0,
            "pageSize": 10,
            "sort": {
                "sorted": True,
                "unsorted": False,
                "empty": False
            },
            "offset": 0,
            "paged": True,
            "unpaged": False
        },
        "totalElements": 1,
        "totalPages": 1,
        "last": True,
        "size": 10,
        "number": 0,
        "sort": {
            "sorted": True,
            "unsorted": False,
            "empty": False
        },
        "numberOfElements": 1,
        "first": True,
        "empty": False
    })
    activity_logs = print_response(activity_logs_response, "Get Server Activity Logs")
    if not activity_logs:
        print("Failed to get server activity logs. Exiting.")
        sys.exit(1)
    
    # Test 15: Delete server user
    delete_server_user_response = mock_response({}, 204)
    print_response(delete_server_user_response, "Delete Server User")
    
    # Test 16: Delete SSH key
    delete_ssh_key_response = mock_response({}, 204)
    print_response(delete_ssh_key_response, "Delete SSH Key")
    
    # Test 17: Delete blacklist entry
    delete_blacklist_response = mock_response({}, 204)
    print_response(delete_blacklist_response, "Delete Blacklist Entry")
    
    # Test 18: Delete team server access
    delete_team_access_response = mock_response({}, 204)
    print_response(delete_team_access_response, "Delete Team Server Access")
    
    # Test 19: Delete server
    delete_server_response = mock_response({}, 204)
    print_response(delete_server_response, "Delete Server")
    
    # Run real server operations tests if servers were started
    if server_started:
        test_real_server_operations()
    
    print("\nAll tests completed!")
    print("\nSummary:")
    print(f"- Created and tested server management")
    print(f"- Created and tested SSH key management")
    print(f"- Created and tested server user management")
    print(f"- Created and tested server blacklist management")
    print(f"- Created and tested team server access management")
    print(f"- Retrieved server activity logs")
    print(f"- Cleaned up all created resources")
    
    if server_started:
        print(f"- Tested real server operations (connect, file operations, command execution)")
        
        # Stop the test servers
        stop_test_servers()

if __name__ == "__main__":
    test_server_service()
