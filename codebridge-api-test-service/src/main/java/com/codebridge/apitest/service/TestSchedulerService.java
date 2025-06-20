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
    private final TaskScheduler taskScheduler;
    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    @Autowired
    public TestSchedulerService(
            ScheduledTestRepository scheduledTestRepository,
            ApiTestService apiTestService,
            TaskScheduler taskScheduler) {
        this.scheduledTestRepository = scheduledTestRepository;
        this.apiTestService = apiTestService;
        this.taskScheduler = taskScheduler;
    }

    /**
     * Initialize scheduled tests on application startup.
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 10000)
    public void initializeScheduledTests() {
        logger.info("Initializing scheduled tests");
        List<ScheduledTest> activeTests = scheduledTestRepository.findByActiveTrue();
        
        for (ScheduledTest test : activeTests) {
            if (!scheduledTasks.containsKey(test.getId())) {
                scheduleTest(test);
            }
        }
    }

    /**
     * Get all scheduled tests for a user.
     *
     * @param userId the user ID
     * @return list of scheduled test responses
     */
    public List<ScheduledTestResponse> getScheduledTests(Long userId) {
        List<ScheduledTest> tests = scheduledTestRepository.findByUserId(userId);
        return tests.stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Get a scheduled test by ID.
     *
     * @param id the scheduled test ID
     * @param userId the user ID
     * @return the scheduled test response
     * @throws ResourceNotFoundException if the scheduled test is not found
     */
    public ScheduledTestResponse getScheduledTest(Long id, Long userId) {
        ScheduledTest test = scheduledTestRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Scheduled test not found"));
        return mapToResponse(test);
    }

    /**
     * Create a new scheduled test.
     *
     * @param request the scheduled test request
     * @param userId the user ID
     * @return the created scheduled test response
     */
    @Transactional
    public ScheduledTestResponse createScheduledTest(ScheduledTestRequest request, Long userId) {
        validateScheduleRequest(request);
        
        ScheduledTest scheduledTest = new ScheduledTest();
        scheduledTest.setTestId(request.getTestId());
        scheduledTest.setEnvironmentId(request.getEnvironmentId());
        scheduledTest.setName(request.getName());
        scheduledTest.setDescription(request.getDescription());
        scheduledTest.setScheduleType(request.getScheduleType());
        scheduledTest.setCronExpression(request.getCronExpression());
        scheduledTest.setIntervalMinutes(request.getIntervalMinutes());
        scheduledTest.setUserId(userId);
        scheduledTest.setActive(true);
        scheduledTest.setLastRunStatus(ScheduledTestStatus.PENDING);
        
        ScheduledTest savedTest = scheduledTestRepository.save(scheduledTest);
        scheduleTest(savedTest);
        
        return mapToResponse(savedTest);
    }

    /**
     * Update a scheduled test.
     *
     * @param id the scheduled test ID
     * @param request the scheduled test request
     * @param userId the user ID
     * @return the updated scheduled test response
     * @throws ResourceNotFoundException if the scheduled test is not found
     */
    @Transactional
    public ScheduledTestResponse updateScheduledTest(Long id, ScheduledTestRequest request, Long userId) {
        validateScheduleRequest(request);
        
        ScheduledTest scheduledTest = scheduledTestRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Scheduled test not found"));
        
        scheduledTest.setTestId(request.getTestId());
        scheduledTest.setEnvironmentId(request.getEnvironmentId());
        scheduledTest.setName(request.getName());
        scheduledTest.setDescription(request.getDescription());
        scheduledTest.setScheduleType(request.getScheduleType());
        scheduledTest.setCronExpression(request.getCronExpression());
        scheduledTest.setIntervalMinutes(request.getIntervalMinutes());
        
        // Cancel existing scheduled task
        cancelScheduledTask(id);
        
        ScheduledTest updatedTest = scheduledTestRepository.save(scheduledTest);
        
        // Reschedule if active
        if (updatedTest.getActive()) {
            scheduleTest(updatedTest);
        }
        
        return mapToResponse(updatedTest);
    }

    /**
     * Activate or deactivate a scheduled test.
     *
     * @param id the scheduled test ID
     * @param active whether to activate or deactivate
     * @param userId the user ID
     * @return the updated scheduled test response
     * @throws ResourceNotFoundException if the scheduled test is not found
     */
    @Transactional
    public ScheduledTestResponse setScheduledTestActive(Long id, boolean active, Long userId) {
        ScheduledTest scheduledTest = scheduledTestRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Scheduled test not found"));
        
        scheduledTest.setActive(active);
        
        // Cancel existing scheduled task
        cancelScheduledTask(id);
        
        ScheduledTest updatedTest = scheduledTestRepository.save(scheduledTest);
        
        // Reschedule if activating
        if (active) {
            scheduleTest(updatedTest);
        }
        
        return mapToResponse(updatedTest);
    }

    /**
     * Delete a scheduled test.
     *
     * @param id the scheduled test ID
     * @param userId the user ID
     * @throws ResourceNotFoundException if the scheduled test is not found
     */
    @Transactional
    public void deleteScheduledTest(Long id, Long userId) {
        ScheduledTest scheduledTest = scheduledTestRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Scheduled test not found"));
        
        // Cancel scheduled task
        cancelScheduledTask(id);
        
        scheduledTestRepository.delete(scheduledTest);
    }

    /**
     * Run a scheduled test immediately.
     *
     * @param id the scheduled test ID
     * @param userId the user ID
     * @return the updated scheduled test response
     * @throws ResourceNotFoundException if the scheduled test is not found
     */
    @Transactional
    public ScheduledTestResponse runScheduledTestNow(Long id, Long userId) {
        ScheduledTest scheduledTest = scheduledTestRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Scheduled test not found"));
        
        executeScheduledTest(scheduledTest);
        
        return mapToResponse(scheduledTest);
    }

    /**
     * Schedule a test based on its configuration.
     *
     * @param test the scheduled test
     */
    private void scheduleTest(ScheduledTest test) {
        if (!test.getActive()) {
            return;
        }
        
        ScheduledFuture<?> future;
        
        if (test.getScheduleType() == ScheduleType.CRON) {
            if (test.getCronExpression() == null || test.getCronExpression().isEmpty()) {
                logger.error("Cannot schedule test with ID {} - missing cron expression", test.getId());
                return;
            }
            
            try {
                CronTrigger trigger = new CronTrigger(test.getCronExpression());
                future = taskScheduler.schedule(() -> executeScheduledTest(test), trigger);
                logger.info("Scheduled test {} with cron expression: {}", test.getId(), test.getCronExpression());
            } catch (IllegalArgumentException e) {
                logger.error("Invalid cron expression for test {}: {}", test.getId(), test.getCronExpression(), e);
                return;
            }
        } else if (test.getScheduleType() == ScheduleType.INTERVAL) {
            if (test.getIntervalMinutes() == null || test.getIntervalMinutes() <= 0) {
                logger.error("Cannot schedule test with ID {} - invalid interval", test.getId());
                return;
            }
            
            long intervalMs = test.getIntervalMinutes() * 60 * 1000L;
            future = taskScheduler.scheduleAtFixedRate(() -> executeScheduledTest(test), intervalMs);
            logger.info("Scheduled test {} with interval: {} minutes", test.getId(), test.getIntervalMinutes());
        } else {
            logger.error("Unknown schedule type for test {}: {}", test.getId(), test.getScheduleType());
            return;
        }
        
        scheduledTasks.put(test.getId(), future);
    }

    /**
     * Cancel a scheduled task.
     *
     * @param testId the scheduled test ID
     */
    private void cancelScheduledTask(Long testId) {
        ScheduledFuture<?> future = scheduledTasks.remove(testId);
        if (future != null) {
            future.cancel(false);
            logger.info("Cancelled scheduled test {}", testId);
        }
    }

    /**
     * Execute a scheduled test.
     *
     * @param test the scheduled test
     */
    private void executeScheduledTest(ScheduledTest test) {
        logger.info("Executing scheduled test: {}", test.getId());
        
        try {
            // Update last run time
            test.setLastRunAt(LocalDateTime.now());
            test.setLastRunStatus(ScheduledTestStatus.RUNNING);
            scheduledTestRepository.save(test);
            
            // Execute the test
            apiTestService.executeTest(
                    test.getTestId(),
                    null, // Project ID will be determined from the test
                    test.getUserId(),
                    test.getEnvironmentId(),
                    null // No additional variables
            );
            
            // Update status to success
            test.setLastRunStatus(ScheduledTestStatus.SUCCESS);
            scheduledTestRepository.save(test);
            
            logger.info("Successfully executed scheduled test: {}", test.getId());
        } catch (Exception e) {
            logger.error("Error executing scheduled test {}: {}", test.getId(), e.getMessage(), e);
            
            // Update status to failure
            test.setLastRunStatus(ScheduledTestStatus.FAILURE);
            test.setLastRunError(e.getMessage());
            scheduledTestRepository.save(test);
        }
    }

    /**
     * Validate a schedule request.
     *
     * @param request the schedule request
     * @throws IllegalArgumentException if the request is invalid
     */
    private void validateScheduleRequest(ScheduledTestRequest request) {
        if (request.getScheduleType() == ScheduleType.CRON) {
            if (request.getCronExpression() == null || request.getCronExpression().isEmpty()) {
                throw new IllegalArgumentException("Cron expression is required for CRON schedule type");
            }
            
            try {
                CronExpression.parse(request.getCronExpression());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid cron expression: " + e.getMessage());
            }
        } else if (request.getScheduleType() == ScheduleType.INTERVAL) {
            if (request.getIntervalMinutes() == null || request.getIntervalMinutes() <= 0) {
                throw new IllegalArgumentException("Interval minutes must be greater than 0");
            }
        } else {
            throw new IllegalArgumentException("Unknown schedule type: " + request.getScheduleType());
        }
    }

    /**
     * Map a scheduled test entity to a response DTO.
     *
     * @param test the scheduled test entity
     * @return the scheduled test response DTO
     */
    private ScheduledTestResponse mapToResponse(ScheduledTest test) {
        ScheduledTestResponse response = new ScheduledTestResponse();
        response.setId(test.getId());
        response.setTestId(test.getTestId());
        response.setEnvironmentId(test.getEnvironmentId());
        response.setName(test.getName());
        response.setDescription(test.getDescription());
        response.setScheduleType(test.getScheduleType());
        response.setCronExpression(test.getCronExpression());
        response.setIntervalMinutes(test.getIntervalMinutes());
        response.setActive(test.getActive());
        response.setLastRunAt(test.getLastRunAt());
        response.setLastRunStatus(test.getLastRunStatus());
        response.setLastRunError(test.getLastRunError());
        response.setCreatedAt(test.getCreatedAt());
        response.setUpdatedAt(test.getUpdatedAt());
        
        // Calculate next run time if possible
        if (test.getActive()) {
            if (test.getScheduleType() == ScheduleType.CRON && test.getCronExpression() != null) {
                try {
                    CronExpression cronExpression = CronExpression.parse(test.getCronExpression());
                    LocalDateTime now = LocalDateTime.now();
                    Optional<LocalDateTime> nextRun = cronExpression.next(now);
                    nextRun.ifPresent(response::setNextRunAt);
                } catch (IllegalArgumentException e) {
                    logger.warn("Could not parse cron expression for test {}: {}", test.getId(), test.getCronExpression());
                }
            } else if (test.getScheduleType() == ScheduleType.INTERVAL && test.getIntervalMinutes() != null) {
                LocalDateTime lastRun = test.getLastRunAt();
                if (lastRun != null) {
                    response.setNextRunAt(lastRun.plusMinutes(test.getIntervalMinutes()));
                } else {
                    // If never run, next run is now
                    response.setNextRunAt(LocalDateTime.now());
                }
            }
        }
        
        return response;
    }
}

