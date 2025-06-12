package com.codebridge.apitest.service;

import com.codebridge.apitest.dto.LoadTestRequest;
import com.codebridge.apitest.dto.LoadTestResult;
import com.codebridge.apitest.dto.TestResultResponse;
import com.codebridge.apitest.exception.ResourceNotFoundException;
import com.codebridge.apitest.exception.TestExecutionException;
import com.codebridge.apitest.model.ApiTest;
import com.codebridge.apitest.model.LoadTest;
import com.codebridge.apitest.model.LoadTestStatus;
import com.codebridge.apitest.model.TestChain;
import com.codebridge.apitest.model.enums.LoadPattern;
import com.codebridge.apitest.repository.ApiTestRepository;
import com.codebridge.apitest.repository.LoadTestRepository;
import com.codebridge.apitest.repository.TestChainRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Service for load testing operations.
 */
@Service
public class LoadTestService {
    
    private static final Logger logger = LoggerFactory.getLogger(LoadTestService.class);
    
    private final LoadTestRepository loadTestRepository;
    private final ApiTestRepository apiTestRepository;
    private final TestChainRepository testChainRepository;
    private final ApiTestService apiTestService;
    private final TestChainService testChainService;
    private final PerformanceMetricsService metricsService;
    private final ExecutorService executorService;
    
    @Autowired
    public LoadTestService(LoadTestRepository loadTestRepository,
                          ApiTestRepository apiTestRepository,
                          TestChainRepository testChainRepository,
                          ApiTestService apiTestService,
                          TestChainService testChainService,
                          PerformanceMetricsService metricsService) {
        this.loadTestRepository = loadTestRepository;
        this.apiTestRepository = apiTestRepository;
        this.testChainRepository = testChainRepository;
        this.apiTestService = apiTestService;
        this.testChainService = testChainService;
        this.metricsService = metricsService;
        
        // Create a thread pool for concurrent test execution
        this.executorService = Executors.newFixedThreadPool(50);
    }
    
    /**
     * Creates a new load test.
     *
     * @param request the load test request
     * @param userId the user ID
     * @return the created load test
     */
    @Transactional
    public LoadTest createLoadTest(LoadTestRequest request, UUID userId) {
        // Validate test or chain exists
        if (request.getTestId() != null) {
            apiTestRepository.findById(request.getTestId())
                .orElseThrow(() -> new ResourceNotFoundException("ApiTest", "id", request.getTestId().toString()));
        } else if (request.getChainId() != null) {
            testChainRepository.findById(request.getChainId())
                .orElseThrow(() -> new ResourceNotFoundException("TestChain", "id", request.getChainId().toString()));
        } else {
            throw new IllegalArgumentException("Either testId or chainId must be provided");
        }
        
        // Create load test
        LoadTest loadTest = new LoadTest();
        loadTest.setId(UUID.randomUUID());
        loadTest.setName(request.getName());
        loadTest.setDescription(request.getDescription());
        loadTest.setUserId(userId);
        loadTest.setTestId(request.getTestId());
        loadTest.setChainId(request.getChainId());
        loadTest.setEnvironmentId(request.getEnvironmentId());
        loadTest.setVirtualUsers(request.getVirtualUsers());
        loadTest.setDurationSeconds(request.getDurationSeconds());
        loadTest.setRampUpSeconds(request.getRampUpSeconds());
        loadTest.setThinkTimeMs(request.getThinkTimeMs());
        
        if (request.getLoadPattern() != null) {
            loadTest.setLoadPattern(LoadPattern.valueOf(request.getLoadPattern()));
        } else {
            loadTest.setLoadPattern(LoadPattern.CONSTANT); // Default to constant load
        }
        
        loadTest.setStatus(LoadTestStatus.CREATED);
        loadTest.setCreatedAt(LocalDateTime.now());
        
        return loadTestRepository.save(loadTest);
    }
    
    /**
     * Gets all load tests for a user.
     *
     * @param userId the user ID
     * @return the list of load tests
     */
    @Transactional(readOnly = true)
    public List<LoadTest> getAllLoadTests(UUID userId) {
        return loadTestRepository.findByUserId(userId);
    }
    
    /**
     * Gets a load test by ID.
     *
     * @param id the load test ID
     * @param userId the user ID
     * @return the load test
     */
    @Transactional(readOnly = true)
    public LoadTest getLoadTestById(UUID id, UUID userId) {
        return loadTestRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new ResourceNotFoundException("LoadTest", "id", id.toString()));
    }
    
    /**
     * Executes a load test asynchronously.
     *
     * @param id the load test ID
     * @param userId the user ID
     */
    @Async
    @Transactional
    public void executeLoadTest(UUID id, UUID userId) {
        LoadTest loadTest = loadTestRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new ResourceNotFoundException("LoadTest", "id", id.toString()));
        
        try {
            // Update status to running
            loadTest.setStatus(LoadTestStatus.RUNNING);
            loadTest.setStartedAt(LocalDateTime.now());
            loadTestRepository.save(loadTest);
            
            // Execute load test
            LoadTestResult result = executeLoadTestInternal(loadTest, userId);
            
            // Update load test with results
            loadTest.setStatus(LoadTestStatus.COMPLETED);
            loadTest.setCompletedAt(LocalDateTime.now());
            loadTest.setTotalRequests(result.getTotalRequests());
            loadTest.setSuccessfulRequests(result.getSuccessfulRequests());
            loadTest.setFailedRequests(result.getFailedRequests());
            loadTest.setAverageResponseTimeMs(result.getAverageResponseTimeMs());
            loadTest.setMinResponseTimeMs(result.getMinResponseTimeMs());
            loadTest.setMaxResponseTimeMs(result.getMaxResponseTimeMs());
            loadTest.setPercentile95Ms(result.getPercentile95Ms());
            loadTest.setPercentile99Ms(result.getPercentile99Ms());
            loadTest.setRequestsPerSecond(result.getRequestsPerSecond());
            loadTest.setErrorRate(result.getErrorRate());
            loadTest.setResultSummary(result.getSummary());
            
            loadTestRepository.save(loadTest);
            
            logger.info("Load test {} completed successfully", id);
        } catch (Exception e) {
            // Update status to failed
            loadTest.setStatus(LoadTestStatus.FAILED);
            loadTest.setCompletedAt(LocalDateTime.now());
            loadTest.setResultSummary("Failed: " + e.getMessage());
            loadTestRepository.save(loadTest);
            
            logger.error("Load test {} failed: {}", id, e.getMessage(), e);
        }
    }
    
    /**
     * Executes a load test internally.
     *
     * @param loadTest the load test
     * @param userId the user ID
     * @return the load test result
     */
    private LoadTestResult executeLoadTestInternal(LoadTest loadTest, UUID userId) {
        int virtualUsers = loadTest.getVirtualUsers();
        int durationSeconds = loadTest.getDurationSeconds();
        int rampUpSeconds = loadTest.getRampUpSeconds() != null ? loadTest.getRampUpSeconds() : 0;
        int thinkTimeMs = loadTest.getThinkTimeMs() != null ? loadTest.getThinkTimeMs() : 0;
        LoadPattern loadPattern = loadTest.getLoadPattern();
        
        logger.info("Starting load test with {} virtual users, {} seconds duration, {} seconds ramp-up, {} ms think time, {} load pattern",
            virtualUsers, durationSeconds, rampUpSeconds, thinkTimeMs, loadPattern);
        
        // Create a list to hold all test results
        List<TestResultResponse> allResults = Collections.synchronizedList(new ArrayList<>());
        
        // Create a countdown latch to wait for all virtual users to complete
        CountDownLatch latch = new CountDownLatch(virtualUsers);
        
        // Create a list to hold all futures
        List<Future<?>> futures = new ArrayList<>();
        
        // Start time for the load test
        long startTimeMs = System.currentTimeMillis();
        
        // Create and submit tasks for each virtual user
        for (int i = 0; i < virtualUsers; i++) {
            final int userIndex = i;
            
            // Calculate delay for this user based on ramp-up time and load pattern
            long delayMs = calculateStartDelay(userIndex, virtualUsers, rampUpSeconds, loadPattern);
            
            // Submit task with delay
            Future<?> future = executorService.submit(() -> {
                try {
                    // Wait for the calculated delay
                    if (delayMs > 0) {
                        Thread.sleep(delayMs);
                    }
                    
                    // Calculate end time for this user
                    long userEndTimeMs = startTimeMs + (durationSeconds * 1000L);
                    
                    // Execute tests until the duration is reached
                    while (System.currentTimeMillis() < userEndTimeMs) {
                        try {
                            // Execute test or chain
                            List<TestResultResponse> results;
                            if (loadTest.getTestId() != null) {
                                TestResultResponse result = apiTestService.executeTest(loadTest.getTestId(), userId);
                                results = List.of(result);
                            } else if (loadTest.getChainId() != null) {
                                results = testChainService.executeTestChain(loadTest.getChainId(), loadTest.getEnvironmentId(), userId);
                            } else {
                                throw new IllegalStateException("Neither testId nor chainId is set");
                            }
                            
                            // Add results to the list
                            allResults.addAll(results);
                            
                            // Think time between requests
                            if (thinkTimeMs > 0) {
                                Thread.sleep(thinkTimeMs);
                            }
                        } catch (Exception e) {
                            logger.error("Error executing test for virtual user {}: {}", userIndex, e.getMessage());
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("Virtual user {} was interrupted", userIndex);
                } finally {
                    latch.countDown();
                }
            });
            
            futures.add(future);
        }
        
        try {
            // Wait for all virtual users to complete or timeout
            boolean completed = latch.await(durationSeconds + rampUpSeconds + 60, TimeUnit.SECONDS);
            if (!completed) {
                logger.warn("Load test timed out waiting for all virtual users to complete");
                
                // Cancel any remaining tasks
                for (Future<?> future : futures) {
                    if (!future.isDone()) {
                        future.cancel(true);
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Load test was interrupted while waiting for completion");
        }
        
        // Calculate end time
        long endTimeMs = System.currentTimeMillis();
        long totalDurationMs = endTimeMs - startTimeMs;
        
        // Calculate metrics
        return calculateLoadTestMetrics(allResults, totalDurationMs);
    }
    
    /**
     * Calculates the start delay for a virtual user based on the load pattern.
     *
     * @param userIndex the user index
     * @param totalUsers the total number of users
     * @param rampUpSeconds the ramp-up time in seconds
     * @param loadPattern the load pattern
     * @return the start delay in milliseconds
     */
    private long calculateStartDelay(int userIndex, int totalUsers, int rampUpSeconds, LoadPattern loadPattern) {
        if (rampUpSeconds <= 0) {
            return 0; // No ramp-up, start all users immediately
        }
        
        switch (loadPattern) {
            case CONSTANT:
                // Distribute users evenly over the ramp-up period
                return (long) (((double) userIndex / totalUsers) * rampUpSeconds * 1000);
                
            case RAMP_UP:
                // More users at the end of the ramp-up period (exponential)
                double factor = Math.pow((double) userIndex / totalUsers, 2);
                return (long) (factor * rampUpSeconds * 1000);
                
            case STEP:
                // Users in steps (e.g., 25%, 50%, 75%, 100%)
                int step = userIndex / (totalUsers / 4 + 1);
                return (long) (((double) step / 4) * rampUpSeconds * 1000);
                
            default:
                return (long) (((double) userIndex / totalUsers) * rampUpSeconds * 1000);
        }
    }
    
    /**
     * Calculates load test metrics from test results.
     *
     * @param results the test results
     * @param totalDurationMs the total duration in milliseconds
     * @return the load test result
     */
    private LoadTestResult calculateLoadTestMetrics(List<TestResultResponse> results, long totalDurationMs) {
        if (results.isEmpty()) {
            return new LoadTestResult(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, "No results collected");
        }
        
        int totalRequests = results.size();
        
        // Count successful and failed requests
        AtomicInteger successfulRequests = new AtomicInteger(0);
        AtomicInteger failedRequests = new AtomicInteger(0);
        
        // Collect response times
        List<Long> responseTimes = new ArrayList<>(totalRequests);
        
        for (TestResultResponse result : results) {
            if (result.getStatus().equals("SUCCESS")) {
                successfulRequests.incrementAndGet();
            } else {
                failedRequests.incrementAndGet();
            }
            
            if (result.getExecutionTimeMs() != null) {
                responseTimes.add(result.getExecutionTimeMs());
            }
        }
        
        // Calculate statistics
        double averageResponseTimeMs = responseTimes.stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0);
        
        long minResponseTimeMs = responseTimes.stream()
            .mapToLong(Long::longValue)
            .min()
            .orElse(0);
        
        long maxResponseTimeMs = responseTimes.stream()
            .mapToLong(Long::longValue)
            .max()
            .orElse(0);
        
        // Sort response times for percentile calculations
        Collections.sort(responseTimes);
        
        // Calculate 95th percentile
        long percentile95Ms = calculatePercentile(responseTimes, 95);
        
        // Calculate 99th percentile
        long percentile99Ms = calculatePercentile(responseTimes, 99);
        
        // Calculate requests per second
        double requestsPerSecond = totalRequests / (totalDurationMs / 1000.0);
        
        // Calculate error rate
        double errorRate = (double) failedRequests.get() / totalRequests * 100;
        
        // Create summary
        String summary = String.format(
            "Total: %d, Success: %d, Failed: %d, Avg: %.2f ms, Min: %d ms, Max: %d ms, 95%%: %d ms, 99%%: %d ms, RPS: %.2f, Error Rate: %.2f%%",
            totalRequests, successfulRequests.get(), failedRequests.get(),
            averageResponseTimeMs, minResponseTimeMs, maxResponseTimeMs,
            percentile95Ms, percentile99Ms, requestsPerSecond, errorRate
        );
        
        return new LoadTestResult(
            totalRequests,
            successfulRequests.get(),
            failedRequests.get(),
            averageResponseTimeMs,
            minResponseTimeMs,
            maxResponseTimeMs,
            percentile95Ms,
            percentile99Ms,
            requestsPerSecond,
            errorRate,
            summary
        );
    }
    
    /**
     * Calculates a percentile value from a sorted list.
     *
     * @param sortedList the sorted list
     * @param percentile the percentile to calculate (0-100)
     * @return the percentile value
     */
    private long calculatePercentile(List<Long> sortedList, int percentile) {
        if (sortedList.isEmpty()) {
            return 0;
        }
        
        int index = (int) Math.ceil(percentile / 100.0 * sortedList.size()) - 1;
        index = Math.max(0, Math.min(index, sortedList.size() - 1));
        return sortedList.get(index);
    }
    
    /**
     * Cancels a running load test.
     *
     * @param id the load test ID
     * @param userId the user ID
     * @return the updated load test
     */
    @Transactional
    public LoadTest cancelLoadTest(UUID id, UUID userId) {
        LoadTest loadTest = loadTestRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new ResourceNotFoundException("LoadTest", "id", id.toString()));
        
        if (loadTest.getStatus() != LoadTestStatus.RUNNING) {
            throw new IllegalStateException("Load test is not running");
        }
        
        loadTest.setStatus(LoadTestStatus.CANCELLED);
        loadTest.setCompletedAt(LocalDateTime.now());
        loadTest.setResultSummary("Cancelled by user");
        
        return loadTestRepository.save(loadTest);
    }
    
    /**
     * Deletes a load test.
     *
     * @param id the load test ID
     * @param userId the user ID
     */
    @Transactional
    public void deleteLoadTest(UUID id, UUID userId) {
        LoadTest loadTest = loadTestRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new ResourceNotFoundException("LoadTest", "id", id.toString()));
        
        if (loadTest.getStatus() == LoadTestStatus.RUNNING) {
            throw new IllegalStateException("Cannot delete a running load test");
        }
        
        loadTestRepository.delete(loadTest);
    }
}

