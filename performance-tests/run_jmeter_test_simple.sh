#!/bin/bash

# Set variables
TEST_PLAN="codebridge_api_test_plan.jmx"
RESULTS_DIR="jmeter_results"
RESULTS_FILE="$RESULTS_DIR/results.jtl"
SUMMARY_FILE="$RESULTS_DIR/summary.txt"
LOG_FILE="$RESULTS_DIR/jmeter.log"

# Create results directory if it doesn't exist
mkdir -p $RESULTS_DIR

# Run JMeter test in non-GUI mode
jmeter -n -t $TEST_PLAN -l $RESULTS_FILE -j $LOG_FILE

# Generate a simple summary
echo "JMeter Test Summary" > $SUMMARY_FILE
echo "===================" >> $SUMMARY_FILE
echo "" >> $SUMMARY_FILE
echo "Test Plan: $TEST_PLAN" >> $SUMMARY_FILE
echo "Date: $(date)" >> $SUMMARY_FILE
echo "" >> $SUMMARY_FILE
echo "Results:" >> $SUMMARY_FILE
echo "--------" >> $SUMMARY_FILE

# Check if the log file exists and contains summary information
if [ -f "$LOG_FILE" ]; then
    grep "summary =" $LOG_FILE | tail -1 >> $SUMMARY_FILE
else
    echo "No log file found. Test may have failed." >> $SUMMARY_FILE
fi

# Check if the results file exists and contains data
if [ -f "$RESULTS_FILE" ]; then
    echo "" >> $SUMMARY_FILE
    echo "Sample Count: $(grep -c "<sample" $RESULTS_FILE)" >> $SUMMARY_FILE
    echo "Error Count: $(grep -c "s=\"false\"" $RESULTS_FILE)" >> $SUMMARY_FILE
else
    echo "No results file found. Test may have failed." >> $SUMMARY_FILE
fi

echo "Test completed. Results saved to $RESULTS_DIR"
echo "Summary:"
cat $SUMMARY_FILE

