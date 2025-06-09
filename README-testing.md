# CodeBridge Testing Guide

This document provides instructions for testing the CodeBridge services using the provided Python test scripts.

## Prerequisites

- Python 3.6 or higher
- Required Python packages:
  - `requests`
  - `websocket-client` (for session service tests)

Install the required packages:

```bash
pip install requests websocket-client
```

## Test Scripts

There are two main test scripts:

1. `codebridge-server-service/test_server_service.py` - Tests the Server Service API
2. `codebridge-session-service/test_session_service.py` - Tests the Session Service API

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

## Running Tests Against Live Services

To run the tests against actual running services, you need to:

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

## Troubleshooting

If you encounter issues running the tests against live services:

1. Ensure the services are running and accessible at the configured URLs
2. Check that the H2 database dependency is properly included in the service's pom.xml
3. Verify that the application-test.yml configuration is properly set up for each service
4. Check the service logs for any errors or exceptions

## Adding New Tests

To add new tests:

1. Add new test methods to the appropriate test script
2. Follow the existing pattern of creating request data, making the API call, and validating the response
3. Update the summary section at the end of the script to include the new tests

