# CodeBridge Performance Testing

This directory contains JMeter test plans and scripts for performance testing the CodeBridge platform.

## Prerequisites

- JMeter 5.5 or higher
- Java 21 or higher

## Test Plans

### API Test Service Test Plan

The `codebridge_api_test_plan.jmx` file contains a test plan for the API Test Service. It includes the following test scenarios:

1. Health check endpoint
2. Get all API tests
3. Create a new API test
4. Execute an API test

## Running the Tests

You can run the tests using the provided shell script:

```bash
cd performance-tests
./run_jmeter_test.sh
```

This will execute the test plan in non-GUI mode and generate results in the `jmeter_results` directory.

## Results

The test results will be saved in the following formats:

- JTL file: `jmeter_results/results.jtl`
- Log file: `jmeter_results/jmeter.log`
- Summary file: `jmeter_results/summary.txt`
- HTML report: `jmeter_results/html_report/`

## Viewing Results

You can view the results in the following ways:

1. Open the HTML report in a web browser
2. View the summary file for a quick overview
3. Open the JTL file in JMeter GUI for detailed analysis

## Customizing Tests

To customize the tests, you can:

1. Open the JMX file in JMeter GUI
2. Modify the test plan as needed
3. Save the changes
4. Run the tests using the shell script

## Troubleshooting

If you encounter any issues running the tests, check the following:

1. Make sure JMeter is installed and in your PATH
2. Verify that the API Test Service is running and accessible
3. Check the JMeter log file for any errors

