#!/bin/bash

# Create results directory
mkdir -p jmeter_results

# Set JMeter path
JMETER_PATH="./apache-jmeter-5.6.3/bin/jmeter"

# Set test duration (in seconds) - shorter for demonstration
TEST_DURATION=60

# Create a modified test plan with shorter duration
sed "s/<stringProp name=\"ThreadGroup.duration\">\${duration}<\/stringProp>/<stringProp name=\"ThreadGroup.duration\">$TEST_DURATION<\/stringProp>/g" codebridge_session_test_plan.jmx > codebridge_session_test_plan_short.jmx

# Run JMeter test with the modified test plan
$JMETER_PATH -n -t codebridge_session_test_plan_short.jmx \
  -l jmeter_results/session_test_results.jtl \
  -j jmeter_results/session_test.log

echo "Test completed. Results saved to jmeter_results directory."

