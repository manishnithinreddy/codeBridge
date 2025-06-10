#!/bin/bash

# Set variables
TEST_PLAN="codebridge_api_test_plan.jmx"
RESULTS_DIR="jmeter_results"
RESULTS_FILE="$RESULTS_DIR/results.jtl"
SUMMARY_FILE="$RESULTS_DIR/summary.txt"
LOG_FILE="$RESULTS_DIR/jmeter.log"
HTML_REPORT_DIR="$RESULTS_DIR/html_report"

# Create results directory if it doesn't exist
mkdir -p $RESULTS_DIR

# Run JMeter test in non-GUI mode
jmeter -n -t $TEST_PLAN -l $RESULTS_FILE -j $LOG_FILE

# Generate HTML report
jmeter -g $RESULTS_FILE -o $HTML_REPORT_DIR

# Generate a simple summary
echo "JMeter Test Summary" > $SUMMARY_FILE
echo "===================" >> $SUMMARY_FILE
echo "" >> $SUMMARY_FILE
echo "Test Plan: $TEST_PLAN" >> $SUMMARY_FILE
echo "Date: $(date)" >> $SUMMARY_FILE
echo "" >> $SUMMARY_FILE
echo "Results:" >> $SUMMARY_FILE
echo "--------" >> $SUMMARY_FILE
grep "summary =" $LOG_FILE | tail -1 >> $SUMMARY_FILE

echo "Test completed. Results saved to $RESULTS_DIR"
echo "Summary:"
cat $SUMMARY_FILE

