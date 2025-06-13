package com.codebridge.performance.service;

import com.codebridge.performance.collector.PerformanceMetricsCollector;
import com.codebridge.performance.model.MetricType;
import com.codebridge.performance.model.PerformanceTest;
import com.codebridge.performance.model.PerformanceTestResult;
import com.codebridge.performance.model.PerformanceTestStatus;
import com.codebridge.performance.repository.PerformanceTestRepository;
import com.codebridge.performance.repository.PerformanceTestResultRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/**
 * Service for managing and executing performance tests.
 */
@Service
@Slf4j
public class PerformanceTestService {

    private static final String SERVICE_NAME = "performance-test";
    
    private final PerformanceTestRepository testRepository;
    private final PerformanceTestResultRepository resultRepository;
    private final PerformanceMetricsCollector metricsCollector;
    private final RestTemplate restTemplate;
    private final ExecutorService executorService;
    
    @Value("${performance.testing.enabled:true}")
    private boolean testingEnabled;
    
    @Value("${performance.testing.concurrent-users:10}")
    private int concurrentUsers;
    
    @Value("${performance.testing.ramp-up-period:60}")
    private int rampUpPeriod;
    
    @Value("${performance.testing.duration:300}")
    private int testDuration;

    @Autowired
    public PerformanceTestService(
            PerformanceTestRepository testRepository,
            PerformanceTestResultRepository resultRepository,
            PerformanceMetricsCollector metricsCollector) {
        this.testRepository = testRepository;
        this.resultRepository = resultRepository;
        this.metricsCollector = metricsCollector;
        this.restTemplate = new RestTemplate();
        this.executorService = Executors.newFixedThreadPool(20);
    }

    /**
     * Scheduled task to run performance tests.
     */
    @Scheduled(cron = "${performance.testing.schedule:0 0 2 * * *}") // 2 AM daily by default
    public void runScheduledTests() {
        if (!testingEnabled) {
            return;
        }
        
        log.info("Running scheduled performance tests");
        
        List<PerformanceTest> scheduledTests = testRepository.findByEnabledAndScheduled(true, true);
        
        for (PerformanceTest test : scheduledTests) {
            try {
                runTest(test.getId());
            } catch (Exception e) {
                log.error("Error running scheduled test {}: {}", test.getId(), e.getMessage(), e);
            }
        }
    }
    
    /**
     * Run a performance test.
     *
     * @param testId the test ID
     * @return the test result
     */
    public PerformanceTestResult runTest(UUID testId) {
        PerformanceTest test = testRepository.findById(testId)
                .orElseThrow(() -> new IllegalArgumentException("Test not found: " + testId));
        
        log.info("Running performance test: {}", test.getName());
        
        // Update test status
        test.setLastRunAt(Instant.now());
        test.setStatus(PerformanceTestStatus.RUNNING);
        testRepository.save(test);
        
        // Create test result
        PerformanceTestResult result = new PerformanceTestResult();
        result.setTest(test);
        result.setStartTime(Instant.now());
        result.setStatus(PerformanceTestStatus.RUNNING);
        result = resultRepository.save(result);
        
        try {
            // Execute the test
            Map<String, Object> testResults = executeTest(test);
            
            // Update test result
            result.setEndTime(Instant.now());
            result.setStatus(PerformanceTestStatus.COMPLETED);
            result.setTotalRequests((Integer) testResults.get("totalRequests"));
            result.setSuccessfulRequests((Integer) testResults.get("successfulRequests"));
            result.setFailedRequests((Integer) testResults.get("failedRequests"));
            result.setAverageResponseTime((Double) testResults.get("averageResponseTime"));
            result.setMinResponseTime((Double) testResults.get("minResponseTime"));
            result.setMaxResponseTime((Double) testResults.get("maxResponseTime"));
            result.setThroughput((Double) testResults.get("throughput"));
            result.setPercentile90((Double) testResults.get("percentile90"));
            result.setPercentile95((Double) testResults.get("percentile95"));
            result.setPercentile99((Double) testResults.get("percentile99"));
            result.setErrorRate((Double) testResults.get("errorRate"));
            
            // Store detailed results as JSON
            result.setDetailedResults(testResults);
            
            // Update test status
            test.setStatus(PerformanceTestStatus.COMPLETED);
            testRepository.save(test);
            
            // Record metrics
            recordTestMetrics(test, result);
            
            // Check for performance regression
            checkPerformanceRegression(test, result);
            
            return resultRepository.save(result);
        } catch (Exception e) {
            log.error("Error executing test {}: {}", testId, e.getMessage(), e);
            
            // Update test result
            result.setEndTime(Instant.now());
            result.setStatus(PerformanceTestStatus.FAILED);
            result.setErrorMessage(e.getMessage());
            
            // Update test status
            test.setStatus(PerformanceTestStatus.FAILED);
            testRepository.save(test);
            
            return resultRepository.save(result);
        }
    }
    
    /**
     * Execute a performance test.
     *
     * @param test the performance test
     * @return the test results
     * @throws Exception if an error occurs
     */
    private Map<String, Object> executeTest(PerformanceTest test) throws Exception {
        // Configure test parameters
        int users = test.getConcurrentUsers() > 0 ? test.getConcurrentUsers() : concurrentUsers;
        int rampUp = test.getRampUpPeriod() > 0 ? test.getRampUpPeriod() : rampUpPeriod;
        int duration = test.getDuration() > 0 ? test.getDuration() : testDuration;
        
        // Create user threads
        List<Callable<List<RequestResult>>> tasks = new ArrayList<>();
        for (int i = 0; i < users; i++) {
            tasks.add(new UserSimulator(test, duration, i, users, rampUp));
        }
        
        // Execute user threads
        List<Future<List<RequestResult>>> futures = executorService.invokeAll(tasks);
        
        // Collect results
        List<RequestResult> allResults = new ArrayList<>();
        for (Future<List<RequestResult>> future : futures) {
            allResults.addAll(future.get());
        }
        
        // Analyze results
        return analyzeResults(allResults);
    }
    
    /**
     * Analyze test results.
     *
     * @param results the list of request results
     * @return the analyzed results
     */
    private Map<String, Object> analyzeResults(List<RequestResult> results) {
        int totalRequests = results.size();
        int successfulRequests = 0;
        int failedRequests = 0;
        double totalResponseTime = 0;
        double minResponseTime = Double.MAX_VALUE;
        double maxResponseTime = 0;
        
        List<Double> responseTimes = new ArrayList<>();
        
        for (RequestResult result : results) {
            if (result.isSuccess()) {
                successfulRequests++;
            } else {
                failedRequests++;
            }
            
            double responseTime = result.getResponseTime();
            totalResponseTime += responseTime;
            minResponseTime = Math.min(minResponseTime, responseTime);
            maxResponseTime = Math.max(maxResponseTime, responseTime);
            
            responseTimes.add(responseTime);
        }
        
        // Calculate statistics
        double averageResponseTime = totalRequests > 0 ? totalResponseTime / totalRequests : 0;
        double errorRate = totalRequests > 0 ? (double) failedRequests / totalRequests * 100 : 0;
        
        // Calculate throughput (requests per second)
        long testDuration = 0;
        if (!results.isEmpty()) {
            long firstRequest = results.get(0).getTimestamp().toEpochMilli();
            long lastRequest = results.get(results.size() - 1).getTimestamp().toEpochMilli();
            testDuration = lastRequest - firstRequest;
        }
        
        double throughput = testDuration > 0 ? 
                (double) totalRequests / (testDuration / 1000.0) : 0;
        
        // Calculate percentiles
        Collections.sort(responseTimes);
        double percentile90 = calculatePercentile(responseTimes, 90);
        double percentile95 = calculatePercentile(responseTimes, 95);
        double percentile99 = calculatePercentile(responseTimes, 99);
        
        // Create result map
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("totalRequests", totalRequests);
        resultMap.put("successfulRequests", successfulRequests);
        resultMap.put("failedRequests", failedRequests);
        resultMap.put("averageResponseTime", averageResponseTime);
        resultMap.put("minResponseTime", minResponseTime);
        resultMap.put("maxResponseTime", maxResponseTime);
        resultMap.put("throughput", throughput);
        resultMap.put("percentile90", percentile90);
        resultMap.put("percentile95", percentile95);
        resultMap.put("percentile99", percentile99);
        resultMap.put("errorRate", errorRate);
        resultMap.put("testDuration", testDuration);
        
        // Add request details
        List<Map<String, Object>> requestDetails = new ArrayList<>();
        for (RequestResult result : results) {
            Map<String, Object> detail = new HashMap<>();
            detail.put("timestamp", result.getTimestamp());
            detail.put("endpoint", result.getEndpoint());
            detail.put("method", result.getMethod());
            detail.put("responseTime", result.getResponseTime());
            detail.put("success", result.isSuccess());
            detail.put("statusCode", result.getStatusCode());
            detail.put("errorMessage", result.getErrorMessage());
            
            requestDetails.add(detail);
        }
        resultMap.put("requestDetails", requestDetails);
        
        return resultMap;
    }
    
    /**
     * Calculate a percentile value.
     *
     * @param values the sorted list of values
     * @param percentile the percentile to calculate (0-100)
     * @return the percentile value
     */
    private double calculatePercentile(List<Double> values, int percentile) {
        if (values.isEmpty()) {
            return 0;
        }
        
        int index = (int) Math.ceil(percentile / 100.0 * values.size()) - 1;
        index = Math.max(0, Math.min(index, values.size() - 1));
        
        return values.get(index);
    }
    
    /**
     * Record test metrics.
     *
     * @param test the performance test
     * @param result the test result
     */
    private void recordTestMetrics(PerformanceTest test, PerformanceTestResult result) {
        Map<String, String> tags = new HashMap<>();
        tags.put("test_id", test.getId().toString());
        tags.put("test_name", test.getName());
        tags.put("test_type", test.getType().name());
        
        // Record test execution metrics
        metricsCollector.recordGauge(SERVICE_NAME, "test.total.requests", result.getTotalRequests(), tags);
        metricsCollector.recordGauge(SERVICE_NAME, "test.successful.requests", result.getSuccessfulRequests(), tags);
        metricsCollector.recordGauge(SERVICE_NAME, "test.failed.requests", result.getFailedRequests(), tags);
        metricsCollector.recordGauge(SERVICE_NAME, "test.error.rate", result.getErrorRate(), tags);
        metricsCollector.recordGauge(SERVICE_NAME, "test.avg.response.time", result.getAverageResponseTime(), tags);
        metricsCollector.recordGauge(SERVICE_NAME, "test.min.response.time", result.getMinResponseTime(), tags);
        metricsCollector.recordGauge(SERVICE_NAME, "test.max.response.time", result.getMaxResponseTime(), tags);
        metricsCollector.recordGauge(SERVICE_NAME, "test.throughput", result.getThroughput(), tags);
        metricsCollector.recordGauge(SERVICE_NAME, "test.percentile.90", result.getPercentile90(), tags);
        metricsCollector.recordGauge(SERVICE_NAME, "test.percentile.95", result.getPercentile95(), tags);
        metricsCollector.recordGauge(SERVICE_NAME, "test.percentile.99", result.getPercentile99(), tags);
        
        // Record test duration
        long testDuration = result.getEndTime().toEpochMilli() - result.getStartTime().toEpochMilli();
        metricsCollector.recordTimer(SERVICE_NAME, "test.duration", testDuration, tags);
    }
    
    /**
     * Check for performance regression.
     *
     * @param test the performance test
     * @param result the test result
     */
    private void checkPerformanceRegression(PerformanceTest test, PerformanceTestResult result) {
        // Get previous test result
        PerformanceTestResult previousResult = resultRepository.findTopByTestAndStatusAndIdNotOrderByEndTimeDesc(
                test, PerformanceTestStatus.COMPLETED, result.getId());
        
        if (previousResult == null) {
            log.info("No previous test result found for comparison");
            return;
        }
        
        // Calculate percentage changes
        double responseTimeChange = calculatePercentageChange(
                previousResult.getAverageResponseTime(), result.getAverageResponseTime());
        
        double throughputChange = calculatePercentageChange(
                previousResult.getThroughput(), result.getThroughput());
        
        double errorRateChange = calculatePercentageChange(
                previousResult.getErrorRate(), result.getErrorRate());
        
        // Check for significant regressions
        boolean hasRegression = false;
        StringBuilder regressionMessage = new StringBuilder("Performance regression detected:\n");
        
        if (responseTimeChange > 10) { // 10% increase in response time
            hasRegression = true;
            regressionMessage.append(String.format(
                    "- Average response time increased by %.2f%% (%.2f ms -> %.2f ms)\n",
                    responseTimeChange, previousResult.getAverageResponseTime(), result.getAverageResponseTime()));
        }
        
        if (throughputChange < -10) { // 10% decrease in throughput
            hasRegression = true;
            regressionMessage.append(String.format(
                    "- Throughput decreased by %.2f%% (%.2f req/s -> %.2f req/s)\n",
                    Math.abs(throughputChange), previousResult.getThroughput(), result.getThroughput()));
        }
        
        if (errorRateChange > 5) { // 5% increase in error rate
            hasRegression = true;
            regressionMessage.append(String.format(
                    "- Error rate increased by %.2f%% (%.2f%% -> %.2f%%)\n",
                    errorRateChange, previousResult.getErrorRate(), result.getErrorRate()));
        }
        
        if (hasRegression) {
            // Update test result with regression information
            result.setHasRegression(true);
            result.setRegressionMessage(regressionMessage.toString());
            
            // Log regression
            log.warn(regressionMessage.toString());
            
            // Record regression metric
            Map<String, String> tags = new HashMap<>();
            tags.put("test_id", test.getId().toString());
            tags.put("test_name", test.getName());
            metricsCollector.incrementCounter(SERVICE_NAME, "test.regression", tags);
        }
    }
    
    /**
     * Calculate percentage change between two values.
     *
     * @param oldValue the old value
     * @param newValue the new value
     * @return the percentage change
     */
    private double calculatePercentageChange(double oldValue, double newValue) {
        if (oldValue == 0) {
            return newValue > 0 ? 100 : 0;
        }
        
        return ((newValue - oldValue) / oldValue) * 100;
    }
    
    /**
     * Create a new performance test.
     *
     * @param name the test name
     * @param description the test description
     * @param type the test type
     * @param endpoint the endpoint to test
     * @param method the HTTP method
     * @param requestBody the request body
     * @param headers the request headers
     * @param concurrentUsers the number of concurrent users
     * @param rampUpPeriod the ramp-up period in seconds
     * @param duration the test duration in seconds
     * @param scheduled whether the test is scheduled
     * @return the created test
     */
    public PerformanceTest createTest(
            String name, String description, PerformanceTest.TestType type,
            String endpoint, String method, String requestBody, Map<String, String> headers,
            int concurrentUsers, int rampUpPeriod, int duration, boolean scheduled) {
        
        PerformanceTest test = new PerformanceTest();
        test.setName(name);
        test.setDescription(description);
        test.setType(type);
        test.setEndpoint(endpoint);
        test.setMethod(method);
        test.setRequestBody(requestBody);
        test.setHeaders(headers);
        test.setConcurrentUsers(concurrentUsers);
        test.setRampUpPeriod(rampUpPeriod);
        test.setDuration(duration);
        test.setScheduled(scheduled);
        test.setEnabled(true);
        test.setStatus(PerformanceTestStatus.CREATED);
        
        return testRepository.save(test);
    }
    
    /**
     * Get all performance tests.
     *
     * @return the list of tests
     */
    public List<PerformanceTest> getAllTests() {
        return testRepository.findAll();
    }
    
    /**
     * Get a performance test by ID.
     *
     * @param id the test ID
     * @return the test
     */
    public PerformanceTest getTestById(UUID id) {
        return testRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Test not found: " + id));
    }
    
    /**
     * Update a performance test.
     *
     * @param id the test ID
     * @param test the updated test
     * @return the updated test
     */
    public PerformanceTest updateTest(UUID id, PerformanceTest test) {
        PerformanceTest existingTest = getTestById(id);
        
        existingTest.setName(test.getName());
        existingTest.setDescription(test.getDescription());
        existingTest.setType(test.getType());
        existingTest.setEndpoint(test.getEndpoint());
        existingTest.setMethod(test.getMethod());
        existingTest.setRequestBody(test.getRequestBody());
        existingTest.setHeaders(test.getHeaders());
        existingTest.setConcurrentUsers(test.getConcurrentUsers());
        existingTest.setRampUpPeriod(test.getRampUpPeriod());
        existingTest.setDuration(test.getDuration());
        existingTest.setScheduled(test.isScheduled());
        existingTest.setEnabled(test.isEnabled());
        
        return testRepository.save(existingTest);
    }
    
    /**
     * Delete a performance test.
     *
     * @param id the test ID
     */
    public void deleteTest(UUID id) {
        testRepository.deleteById(id);
    }
    
    /**
     * Get test results for a test.
     *
     * @param testId the test ID
     * @return the list of test results
     */
    public List<PerformanceTestResult> getTestResults(UUID testId) {
        PerformanceTest test = getTestById(testId);
        return resultRepository.findByTestOrderByStartTimeDesc(test);
    }
    
    /**
     * Get a test result by ID.
     *
     * @param id the result ID
     * @return the test result
     */
    public PerformanceTestResult getTestResultById(UUID id) {
        return resultRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Test result not found: " + id));
    }
    
    /**
     * Inner class representing a simulated user for load testing.
     */
    private class UserSimulator implements Callable<List<RequestResult>> {
        private final PerformanceTest test;
        private final int duration;
        private final int userIndex;
        private final int totalUsers;
        private final int rampUpPeriod;
        
        public UserSimulator(PerformanceTest test, int duration, int userIndex, int totalUsers, int rampUpPeriod) {
            this.test = test;
            this.duration = duration;
            this.userIndex = userIndex;
            this.totalUsers = totalUsers;
            this.rampUpPeriod = rampUpPeriod;
        }
        
        @Override
        public List<RequestResult> call() throws Exception {
            List<RequestResult> results = new ArrayList<>();
            
            // Calculate start delay based on ramp-up period
            long startDelay = rampUpPeriod > 0 ? 
                    (long) (rampUpPeriod * 1000 * (userIndex / (double) totalUsers)) : 0;
            
            if (startDelay > 0) {
                Thread.sleep(startDelay);
            }
            
            // Calculate end time
            long endTime = System.currentTimeMillis() + (duration * 1000);
            
            // Execute requests until duration is reached
            while (System.currentTimeMillis() < endTime) {
                try {
                    RequestResult result = executeRequest();
                    results.add(result);
                    
                    // Add think time between requests (random between 100ms and 1000ms)
                    Thread.sleep(ThreadLocalRandom.current().nextInt(100, 1001));
                } catch (Exception e) {
                    log.error("Error executing request: {}", e.getMessage(), e);
                }
            }
            
            return results;
        }
        
        private RequestResult executeRequest() {
            RequestResult result = new RequestResult();
            result.setTimestamp(Instant.now());
            result.setEndpoint(test.getEndpoint());
            result.setMethod(test.getMethod());
            
            long startTime = System.currentTimeMillis();
            
            try {
                // Execute the request based on the test type
                switch (test.getType()) {
                    case HTTP:
                        executeHttpRequest(result);
                        break;
                    case API:
                        executeApiRequest(result);
                        break;
                    case DATABASE:
                        executeDatabaseRequest(result);
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported test type: " + test.getType());
                }
                
                result.setResponseTime(System.currentTimeMillis() - startTime);
                return result;
            } catch (Exception e) {
                result.setSuccess(false);
                result.setErrorMessage(e.getMessage());
                result.setResponseTime(System.currentTimeMillis() - startTime);
                return result;
            }
        }
        
        private void executeHttpRequest(RequestResult result) {
            // In a real implementation, this would use RestTemplate or WebClient
            // to execute an HTTP request to the specified endpoint
            
            // Simulate a request
            result.setSuccess(true);
            result.setStatusCode(200);
        }
        
        private void executeApiRequest(RequestResult result) {
            // In a real implementation, this would use a client to execute
            // an API request to the specified endpoint
            
            // Simulate a request
            result.setSuccess(true);
            result.setStatusCode(200);
        }
        
        private void executeDatabaseRequest(RequestResult result) {
            // In a real implementation, this would use a database client
            // to execute a database query
            
            // Simulate a request
            result.setSuccess(true);
            result.setStatusCode(200);
        }
    }
    
    /**
     * Inner class representing a request result.
     */
    private static class RequestResult {
        private Instant timestamp;
        private String endpoint;
        private String method;
        private double responseTime;
        private boolean success;
        private int statusCode;
        private String errorMessage;
        
        public Instant getTimestamp() {
            return timestamp;
        }
        
        public void setTimestamp(Instant timestamp) {
            this.timestamp = timestamp;
        }
        
        public String getEndpoint() {
            return endpoint;
        }
        
        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }
        
        public String getMethod() {
            return method;
        }
        
        public void setMethod(String method) {
            this.method = method;
        }
        
        public double getResponseTime() {
            return responseTime;
        }
        
        public void setResponseTime(double responseTime) {
            this.responseTime = responseTime;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public void setSuccess(boolean success) {
            this.success = success;
        }
        
        public int getStatusCode() {
            return statusCode;
        }
        
        public void setStatusCode(int statusCode) {
            this.statusCode = statusCode;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }
}

