# CodeBridge Testing Guide

This document provides instructions for testing the CodeBridge services using the provided Python test scripts.

## Prerequisites

- Python 3.6 or higher
- Required Python packages:
  - `requests`
  - `websocket-client` (for session service tests)
  - `paramiko` (for SSH operations)
  - `pyftpdlib` (for FTP server)

Install the required packages:

```bash
pip install requests websocket-client paramiko pyftpdlib
```

## Test Scripts

There are two main test scripts:

1. `codebridge-server-service/test_server_service.py` - Tests the Server Service API
2. `codebridge-session-service/test_session_service.py` - Tests the Session Service API

Additionally, there are test server scripts in the `test_server` directory:

1. `test_server/ssh_server.py` - A simple SSH server for testing
2. `test_server/ftp_server.py` - A simple FTP server for testing
3. `test_server/start_servers.py` - Script to start both servers
4. `test_server/test_file_operations.py` - Standalone script to test file operations

## Running Tests with Mock Data

Both test scripts can be run in "mock mode" without requiring the actual services to be running. This is useful for verifying the expected API behavior and response formats.

### Server Service Tests

```bash
cd codebridge-server-service
python test_server_service.py
```

This will run through all the server service API endpoints with mock data, including:
- Server management (create, read, update, delete)
- SSH key management
- Server user access control
- Server blacklist management
- Team server access management
- Activity log retrieval

### Session Service Tests

```bash
cd codebridge-session-service
python test_session_service.py
```

This will run through all the session service API endpoints with mock data, including:
- SSH session management
- Database session management
- SQL query execution
- File operations via SSH
- Host key management
- WebSocket connection testing (simulated)

## Running Tests with Real Servers

The test scripts now include functionality to start local test servers for SSH and FTP operations. This allows you to test real file operations without needing external servers.

### Prerequisites for Real Server Tests

1. Make sure the test server scripts are in the `test_server` directory:
   - `ssh_server.py`
   - `ftp_server.py`
   - `start_servers.py`

2. Install the required packages:
   ```bash
   pip install paramiko pyftpdlib
   ```

### Running the Tests with Real Servers

The main test scripts will automatically attempt to start the test servers and run real operations if possible:

```bash
cd codebridge-server-service
python test_server_service.py
```

```bash
cd codebridge-session-service
python test_session_service.py
```

If the test servers start successfully, the scripts will perform real file operations including:
- Connecting to SSH server
- Executing commands
- Creating directories
- Creating files
- Uploading files
- Downloading files
- Deleting files

### Running the File Operations Test Directly

You can also run the file operations test script directly:

```bash
cd test_server
python test_file_operations.py
```

This script will:
1. Start the SSH and FTP servers
2. Run comprehensive tests for SSH operations
3. Run comprehensive tests for FTP operations
4. Stop the servers when done

## Running Tests Against Live Services

To run the tests against actual running CodeBridge services:

1. Start the server service:
   ```bash
   cd codebridge-server-service
   mvn spring-boot:run -Dspring-boot.run.profiles=test -Dspring-boot.run.jvmArguments="-Djasypt.encryptor.password=test-password"
   ```

2. Start the session service:
   ```bash
   cd codebridge-session-service
   mvn spring-boot:run -Dspring-boot.run.profiles=test -Dspring-boot.run.jvmArguments="-Djasypt.encryptor.password=test-password"
   ```

3. Modify the test scripts to use real API calls instead of mock data:
   - In both test scripts, comment out the `mock_response` calls and uncomment the actual `requests` calls.

## Test Configuration

You can modify the following parameters in the test scripts:

- `BASE_URL`: The base URL for the service API
- `MOCK_JWT_TOKEN`: The JWT token used for authentication
- `SSH_HOST`, `SSH_PORT`, `SSH_USERNAME`, `SSH_PASSWORD`: SSH server connection details
- `FTP_HOST`, `FTP_PORT`, `FTP_USERNAME`, `FTP_PASSWORD`: FTP server connection details

## Troubleshooting

If you encounter issues running the tests:

1. Ensure the services are running and accessible at the configured URLs
2. Check that the H2 database dependency is properly included in the service's pom.xml
3. Verify that the application-test.yml configuration is properly set up for each service
4. Check the service logs for any errors or exceptions
5. For test server issues:
   - Ensure all required packages are installed
   - Check if the ports (2222 for SSH, 2121 for FTP) are available
   - Look for error messages in the console output

## Adding New Tests

To add new tests:

1. Add new test methods to the appropriate test script
2. Follow the existing pattern of creating request data, making the API call, and validating the response
3. Update the summary section at the end of the script to include the new tests
4. For real server operations, add new test cases to the appropriate test function

