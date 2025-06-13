package com.codebridge.apitest.service;

import com.codebridge.apitest.dto.ScheduledTestRequest;
import com.codebridge.apitest.dto.ScheduledTestResponse;
import com.codebridge.apitest.exception.ResourceNotFoundException;
import com.codebridge.apitest.model.ScheduledTest;
import com.codebridge.apitest.model.ScheduledTestStatus;
import com.codebridge.apitest.model.enums.ScheduleType;
import com.codebridge.apitest.repository.ScheduledTestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * Service for test scheduling operations.
 */
@Service
public class TestSchedulerService {
    
    private static final Logger logger = LoggerFactory.getLogger(TestSchedulerService.class);
    
    private final ScheduledTestRepository scheduledTestRepository;
    private final ApiTestService apiTestService;
    private final TestChainService testChainService;
    private final LoadTestService loadTestService;
    private final TaskScheduler taskScheduler;
    
    // Map to store scheduled tasks
    private final Map<UUID, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    
    @Autowired
    public TestSchedulerService(ScheduledTestRepository scheduledTestRepository,
                               ApiTestService apiTestService,
                               TestChainService testChainService,
                               LoadTestService loadTestService,
                               TaskScheduler taskScheduler) {
        this.scheduledTestRepository = scheduledTestRepository;
        this.apiTestService = apiTestService;
        this.testChainService = testChainService;
        this.loadTestService = loadTestService;
        this.taskScheduler = taskScheduler;
    }
    
    /**
     * Creates a new scheduled test.
     *
     * @param request the scheduled test request
     * @param userId the user ID
     * @return the created scheduled test
     */
    @Transactional
    public ScheduledTest createScheduledTest(ScheduledTestRequest request, UUID userId) {
        // Validate schedule expression
        if (request.getScheduleType() == ScheduleType.CRON && !isValidCronExpression(request.getCronExpression())) {
            throw new IllegalArgumentException("Invalid cron expression: " + request.getCronExpression());
        }
        
        // Create scheduled test
        ScheduledTest scheduledTest = new ScheduledTest();
        scheduledTest.setId(UUID.randomUUID());
        scheduledTest.setName(request.getName());
        scheduledTest.setDescription(request.getDescription());
        scheduledTest.setUserId(userId);
        scheduledTest.setTestId(request.getTestId());
        scheduledTest.setChainId(request.getChainId());
        scheduledTest.setLoadTestId(request.getLoadTestId());
        scheduledTest.setEnvironmentId(request.getEnvironmentId());
        scheduledTest.setScheduleType(request.getScheduleType());
        scheduledTest.setCronExpression(request.getCronExpression());
        scheduledTest.setFixedRateSeconds(request.getFixedRateSeconds());
        scheduledTest.setOneTimeExecutionTime(request.getOneTimeExecutionTime());
        scheduledTest.setWebhookUrl(request.getWebhookUrl());
        scheduledTest.setActive(true);
        scheduledTest.setStatus(ScheduledTestStatus.CREATED);
        scheduledTest.setCreatedAt(LocalDateTime.now());
        
        ScheduledTest savedScheduledTest = scheduledTestRepository.save(scheduledTest);
        
        // Schedule the test
        scheduleTest(savedScheduledTest);
        
        return savedScheduledTest;
    }
    
    /**
     * Gets all scheduled tests for a user.
     *
     * @param userId the user ID
     * @return the list of scheduled tests
     */
    @Transactional(readOnly = true)
    public List<ScheduledTest> getAllScheduledTests(UUID userId) {
        return scheduledTestRepository.findByUserId(userId);
    }
    
    /**
     * Gets a scheduled test by ID.
     *
     * @param id the scheduled test ID
     * @param userId the user ID
     * @return the scheduled test
     */
    @Transactional(readOnly = true)
    public ScheduledTest getScheduledTestById(UUID id, UUID userId) {
        return scheduledTestRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new ResourceNotFoundException("ScheduledTest", "id", id.toString()));
    }
    
    /**
     * Updates a scheduled test.
     *
     * @param id the scheduled test ID
     * @param request the scheduled test request
     * @param userId the user ID
     * @return the updated scheduled test
     */
    @Transactional
    public ScheduledTest updateScheduledTest(UUID id, ScheduledTestRequest request, UUID userId) {
        ScheduledTest scheduledTest = scheduledTestRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new ResourceNotFoundException("ScheduledTest", "id", id.toString()));
        
        // Validate schedule expression
        if (request.getScheduleType() == ScheduleType.CRON && !isValidCronExpression(request.getCronExpression())) {
            throw new IllegalArgumentException("Invalid cron expression: " + request.getCronExpression());
        }
        
        // Cancel existing schedule
        cancelScheduledTask(id);
        
        // Update scheduled test
        scheduledTest.setName(request.getName());
        scheduledTest.setDescription(request.getDescription());
        scheduledTest.setTestId(request.getTestId());
        scheduledTest.setChainId(request.getChainId());
        scheduledTest.setLoadTestId(request.getLoadTestId());
        scheduledTest.setEnvironmentId(request.getEnvironmentId());
        scheduledTest.setScheduleType(request.getScheduleType());
        scheduledTest.setCronExpression(request.getCronExpression());
        scheduledTest.setFixedRateSeconds(request.getFixedRateSeconds());
        scheduledTest.setOneTimeExecutionTime(request.getOneTimeExecutionTime());
        scheduledTest.setWebhookUrl(request.getWebhookUrl());
        scheduledTest.setUpdatedAt(LocalDateTime.now());
        
        ScheduledTest updatedScheduledTest = scheduledTestRepository.save(scheduledTest);
        
        // Reschedule the test if active
        if (updatedScheduledTest.isActive()) {
            scheduleTest(updatedScheduledTest);
        }
        
        return updatedScheduledTest;
    }
    
    /**
     * Activates or deactivates a scheduled test.
     *
     * @param id the scheduled test ID
     * @param active whether the test should be active
     * @param userId the user ID
     * @return the updated scheduled test
     */
    @Transactional
    public ScheduledTest setScheduledTestActive(UUID id, boolean active, UUID userId) {
        ScheduledTest scheduledTest = scheduledTestRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new ResourceNotFoundException("ScheduledTest", "id", id.toString()));
        
        if (scheduledTest.isActive() == active) {
            return scheduledTest; // No change needed
        }
        
        scheduledTest.setActive(active);
        scheduledTest.setUpdatedAt(LocalDateTime.now());
        
        if (active) {
            // Activate: schedule the test
            scheduleTest(scheduledTest);
        } else {
            // Deactivate: cancel the scheduled task
            cancelScheduledTask(id);
        }
        
        return scheduledTestRepository.save(scheduledTest);
    }
    
    /**
     * Deletes a scheduled test.
     *
     * @param id the scheduled test ID
     * @param userId the user ID
     */
    @Transactional
    public void deleteScheduledTest(UUID id, UUID userId) {
        ScheduledTest scheduledTest = scheduledTestRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new ResourceNotFoundException("ScheduledTest", "id", id.toString()));
        
        // Cancel the scheduled task
        cancelScheduledTask(id);
        
        // Delete the scheduled test
        scheduledTestRepository.delete(scheduledTest);
    }
    
    /**
     * Executes a scheduled test immediately.
     *
     * @param id the scheduled test ID
     * @param userId the user ID
     * @return the updated scheduled test
     */
    @Transactional
    public ScheduledTest executeScheduledTestNow(UUID id, UUID userId) {
        ScheduledTest scheduledTest = scheduledTestRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new ResourceNotFoundException("ScheduledTest", "id", id.toString()));
        
        // Execute the test
        executeScheduledTest(scheduledTest);
        
        return scheduledTest;
    }
    
    /**
     * Schedules a test based on its schedule type.
     *
     * @param scheduledTest the scheduled test
     */
    private void scheduleTest(ScheduledTest scheduledTest) {
        // Cancel any existing scheduled task
        cancelScheduledTask(scheduledTest.getId());
        
        // Schedule based on schedule type
        ScheduledFuture<?> future = null;
        
        switch (scheduledTest.getScheduleType()) {
            case CRON:
                if (scheduledTest.getCronExpression() != null && !scheduledTest.getCronExpression().isEmpty()) {
                    future = taskScheduler.schedule(
                        () -> executeScheduledTest(scheduledTest),
                        new CronTrigger(scheduledTest.getCronExpression())
                    );
                    logger.info("Scheduled test {} with cron expression: {}", scheduledTest.getId(), scheduledTest.getCronExpression());
                }
                break;
                
            case FIXED_RATE:
                if (scheduledTest.getFixedRateSeconds() != null && scheduledTest.getFixedRateSeconds() > 0) {
                    future = taskScheduler.scheduleAtFixedRate(
                        () -> executeScheduledTest(scheduledTest),
                        scheduledTest.getFixedRateSeconds() * 1000L
                    );
                    logger.info("Scheduled test {} with fixed rate: {} seconds", scheduledTest.getId(), scheduledTest.getFixedRateSeconds());
                }
                break;
                
            case ONE_TIME:
                if (scheduledTest.getOneTimeExecutionTime() != null) {
                    Date executionTime = Date.from(scheduledTest.getOneTimeExecutionTime().atZone(ZoneId.systemDefault()).toInstant());
                    future = taskScheduler.schedule(
                        () -> executeScheduledTest(scheduledTest),
                        executionTime
                    );
                    logger.info("Scheduled test {} for one-time execution at: {}", scheduledTest.getId(), scheduledTest.getOneTimeExecutionTime());
                }
                break;
                
            default:
                logger.warn("Unknown schedule type for test {}: {}", scheduledTest.getId(), scheduledTest.getScheduleType());
        }
        
        // Store the scheduled future
        if (future != null) {
            scheduledTasks.put(scheduledTest.getId(), future);
        }
    }
    
    /**
     * Cancels a scheduled task.
     *
     * @param id the scheduled test ID
     */
    private void cancelScheduledTask(UUID id) {
        ScheduledFuture<?> future = scheduledTasks.remove(id);
        if (future != null) {
            future.cancel(false);
            logger.info("Cancelled scheduled task for test {}", id);
        }
    }
    
    /**
     * Executes a scheduled test.
     *
     * @param scheduledTest the scheduled test
     */
    private void executeScheduledTest(ScheduledTest scheduledTest) {
        try {
            logger.info("Executing scheduled test: {}", scheduledTest.getId());
            
            // Update status to running
            scheduledTest.setStatus(ScheduledTestStatus.RUNNING);
            scheduledTest.setLastExecutionStartTime(LocalDateTime.now());
            scheduledTestRepository.save(scheduledTest);
            
            // Execute based on test type
            if (scheduledTest.getTestId() != null) {
                // Execute API test
                apiTestService.executeTest(scheduledTest.getTestId(), scheduledTest.getUserId());
            } else if (scheduledTest.getChainId() != null) {
                // Execute test chain
                testChainService.executeTestChain(scheduledTest.getChainId(), scheduledTest.getEnvironmentId(), scheduledTest.getUserId());
            } else if (scheduledTest.getLoadTestId() != null) {
                // Execute load test
                loadTestService.executeLoadTest(scheduledTest.getLoadTestId(), scheduledTest.getUserId());
            } else {
                throw new IllegalStateException("No test, chain, or load test specified for scheduled test " + scheduledTest.getId());
            }
            
            // Update status to completed
            scheduledTest.setStatus(ScheduledTestStatus.COMPLETED);
            scheduledTest.setLastExecutionEndTime(LocalDateTime.now());
            scheduledTest.setLastExecutionSuccess(true);
            scheduledTest.setExecutionCount(scheduledTest.getExecutionCount() + 1);
            scheduledTestRepository.save(scheduledTest);
            
            // Send webhook notification if configured
            if (scheduledTest.getWebhookUrl() != null && !scheduledTest.getWebhookUrl().isEmpty()) {
                sendWebhookNotification(scheduledTest, true, null);
            }
            
            logger.info("Scheduled test {} executed successfully", scheduledTest.getId());
        } catch (Exception e) {
            logger.error("Error executing scheduled test {}: {}", scheduledTest.getId(), e.getMessage(), e);
            
            // Update status to failed
            scheduledTest.setStatus(ScheduledTestStatus.FAILED);
            scheduledTest.setLastExecutionEndTime(LocalDateTime.now());
            scheduledTest.setLastExecutionSuccess(false);
            scheduledTest.setLastErrorMessage(e.getMessage());
            scheduledTest.setExecutionCount(scheduledTest.getExecutionCount() + 1);
            scheduledTestRepository.save(scheduledTest);
            
            // Send webhook notification if configured
            if (scheduledTest.getWebhookUrl() != null && !scheduledTest.getWebhookUrl().isEmpty()) {
                sendWebhookNotification(scheduledTest, false, e.getMessage());
            }
        }
    }
    
    /**
     * Sends a webhook notification for a scheduled test execution.
     *
     * @param scheduledTest the scheduled test
     * @param success whether the execution was successful
     * @param errorMessage the error message if the execution failed
     */
    private void sendWebhookNotification(ScheduledTest scheduledTest, boolean success, String errorMessage) {
        // This would be implemented with an HTTP client to send a POST request to the webhook URL
        // For now, we'll just log the notification
        logger.info("Webhook notification for scheduled test {}: success={}, error={}", 
            scheduledTest.getId(), success, errorMessage);
    }
    
    /**
     * Checks if a cron expression is valid.
     *
     * @param cronExpression the cron expression
     * @return true if the expression is valid, false otherwise
     */
    private boolean isValidCronExpression(String cronExpression) {
        if (cronExpression == null || cronExpression.isEmpty()) {
            return false;
        }
        
        try {
            CronExpression.parse(cronExpression);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * Scheduled task to check for and execute one-time scheduled tests.
     * Runs every minute.
     */
    @Scheduled(fixedRate = 60000)
    public void checkOneTimeScheduledTests() {
        logger.debug("Checking for one-time scheduled tests");
        
        LocalDateTime now = LocalDateTime.now();
        List<ScheduledTest> oneTimeTests = scheduledTestRepository.findByScheduleTypeAndActiveTrue(ScheduleType.ONE_TIME);
        
        for (ScheduledTest test : oneTimeTests) {
            if (test.getOneTimeExecutionTime() != null && 
                test.getOneTimeExecutionTime().isBefore(now) && 
                (test.getLastExecutionStartTime() == null || test.getStatus() == ScheduledTestStatus.FAILED)) {
                
                logger.info("Executing one-time scheduled test {}", test.getId());
                executeScheduledTest(test);
            }
        }
    }
    
    /**
     * Initializes all active scheduled tests on application startup.
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 10000)
    public void initializeScheduledTests() {
        // Only run once on startup
        if (scheduledTasks.isEmpty()) {
            logger.info("Initializing scheduled tests");
            
            List<ScheduledTest> activeTests = scheduledTestRepository.findByActiveTrue();
            for (ScheduledTest test : activeTests) {
                scheduleTest(test);
            }
            
            logger.info("Initialized {} scheduled tests", activeTests.size());
        }
    }
}

