# JMeter Performance Testing for CodeBridge Session Service

This document provides instructions for running performance tests on the CodeBridge Session Service using Apache JMeter.

## Test Plan Overview

The JMeter test plan (`codebridge_session_test_plan.jmx`) is designed to test the following endpoints:

### Health Check Endpoints
- `GET /api/health` - Basic health check
- `GET /api/health/details` - Detailed health check with resource usage information

### DB Session Lifecycle Endpoints
- `POST /api/lifecycle/db/init` - Initialize a new DB session
- `POST /api/lifecycle/db/{sessionToken}/keepalive` - Keep a DB session alive
- `POST /api/lifecycle/db/{sessionToken}/release` - Release a DB session

### File Transfer Endpoints
- `POST /api/transfers/initialize` - Initialize a file upload
- `GET /api/transfers/{transferId}/status` - Get the status of a file transfer

## Thread Groups

The test plan includes three thread groups:

1. **Health Check Thread Group**: 10 concurrent users checking the health endpoints
2. **DB Session Lifecycle Thread Group**: 50 concurrent users performing DB session operations
3. **File Transfer Thread Group**: 20 concurrent users performing file transfer operations

## Test Configuration

The test plan includes configurable parameters:

- `host`: Target host (default: localhost)
- `port`: Target port (default: 8080)
- `protocol`: HTTP protocol (default: http)
- `threads`: Number of concurrent users for DB operations (default: 50)
- `rampup`: Ramp-up period in seconds (default: 30)
- `duration`: Test duration in seconds (default: 300)
- `token`: Authentication token (default: test-session-token)

## Running the Tests

### Prerequisites

- Apache JMeter 5.6.3 or later
- Java 8 or later

### Running the Test

1. Use the provided script to run the test with a shorter duration:

```bash
./run_jmeter_test.sh
```

This script:
- Creates a modified test plan with a shorter duration (60 seconds)
- Runs the test in non-GUI mode
- Saves results to the `jmeter_results` directory

### Running the Full Test

To run the full test with the default duration (300 seconds):

```bash
./apache-jmeter-5.6.3/bin/jmeter -n -t codebridge_session_test_plan.jmx \
  -l jmeter_results/session_test_results.jtl \
  -j jmeter_results/session_test.log
```

### Generating HTML Reports

To generate an HTML report from the test results:

```bash
./apache-jmeter-5.6.3/bin/jmeter -g jmeter_results/session_test_results.jtl \
  -o jmeter_results/html_report
```

## Analyzing Results

The test generates the following result files:

- `session_test_results.jtl`: Raw test results in JTL format
- `session_test.log`: Test execution log
- HTML report (if generated): Detailed performance metrics and graphs

Key metrics to analyze:

- **Throughput**: Requests per second
- **Response Time**: Average, median, 90th percentile, and 95th percentile response times
- **Error Rate**: Percentage of failed requests
- **Latency**: Time to first byte

## Modifying the Test Plan

To modify the test plan:

1. Open the JMX file in JMeter GUI:
```bash
./apache-jmeter-5.6.3/bin/jmeter -t codebridge_session_test_plan.jmx
```

2. Make your changes and save the file
3. Run the test using the instructions above

## Troubleshooting

- If the test fails to connect, verify the host and port settings
- If authentication fails, update the token value in the User Defined Variables
- For memory issues, adjust JMeter memory settings in `jmeter.bat` or `jmeter.sh`

