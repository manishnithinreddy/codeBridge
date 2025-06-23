package com.codebridge.apitest.service;

import com.codebridge.apitest.dto.ScheduledTestRequest;
import com.codebridge.apitest.dto.ScheduledTestResponse;
import com.codebridge.apitest.dto.TestResultResponse;
import com.codebridge.apitest.exception.ResourceNotFoundException;
import com.codebridge.apitest.model.ScheduledTest;
import com.codebridge.apitest.model.ScheduledTestStatus;
import com.codebridge.apitest.model.enums.ScheduleType;
import com.codebridge.apitest.repository.ScheduledTestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for scheduled test operations.
 */
@Service
public class TestSchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(TestSchedulerService.class);

    private final ScheduledTestRepository scheduledTestRepository;
    private final ApiTestService apiTestService;

    @Autowired
    public TestSchedulerService(ScheduledTestRepository scheduledTestRepository, ApiTestService apiTestService) {
        this.scheduledTestRepository = scheduledTestRepository;
        this.apiTestService = apiTestService;
    }

    /**
     * Create a scheduled test.
     *
     * @param request the scheduled test request
     * @param userId the user ID
     * @return the created scheduled test
     */
    @Transactional
    public ScheduledTestResponse createScheduledTest(ScheduledTestRequest request, Long userId) {
        validateScheduleRequest(request);

        ScheduledTest scheduledTest = new ScheduledTest();
        scheduledTest.setName(request.getName());
        scheduledTest.setDescription(request.getDescription());
        scheduledTest.setUserId(userId);
        scheduledTest.setTestId(request.getTestId());
        scheduledTest.setEnvironmentId(request.getEnvironmentId());
        scheduledTest.setScheduleType(request.getScheduleType());
        scheduledTest.setEnabled(request.isEnabled());
        scheduledTest.setStatus(ScheduledTestStatus.IDLE);

        // Set schedule-specific fields
        if (request.getScheduleType() == ScheduleType.CRON) {
            scheduledTest.setCronExpression(request.getCronExpression());
        } else if (request.getScheduleType() == ScheduleType.FIXED_RATE) {
            scheduledTest.setFixedRateSeconds(request.getFixedRateSeconds());
        } else if (request.getScheduleType() == ScheduleType.INTERVAL) {
            scheduledTest.setIntervalMinutes(request.getIntervalMinutes());
        } else if (request.getScheduleType() == ScheduleType.ONE_TIME) {
            scheduledTest.setOneTimeExecutionTime(request.getOneTimeExecutionTime());
        }

        scheduledTest.setWebhookUrl(request.getWebhookUrl());
        
        ScheduledTest savedTest = scheduledTestRepository.save(scheduledTest);
        return mapToResponse(savedTest);
    }

    /**
     * Get a scheduled test by ID.
     *
     * @param id the scheduled test ID
     * @param userId the user ID
     * @return the scheduled test
     * @throws ResourceNotFoundException if the scheduled test is not found
     */
    public ScheduledTestResponse getScheduledTestById(Long id, Long userId) {
        ScheduledTest test = scheduledTestRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("ScheduledTest", "id", id));
        return mapToResponse(test);
    }

    /**
     * Get all scheduled tests for a user.
     *
     * @param userId the user ID
     * @return the list of scheduled tests
     */
    public List<ScheduledTestResponse> getAllScheduledTestsForUser(Long userId) {
        List<ScheduledTest> tests = scheduledTestRepository.findByUserId(userId);
        return tests.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Update a scheduled test.
     *
     * @param id the scheduled test ID
     * @param request the scheduled test request
     * @param userId the user ID
     * @return the updated scheduled test
     * @throws ResourceNotFoundException if the scheduled test is not found
     */
    @Transactional
    public ScheduledTestResponse updateScheduledTest(Long id, ScheduledTestRequest request, Long userId) {
        validateScheduleRequest(request);

        ScheduledTest test = scheduledTestRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("ScheduledTest", "id", id));

        test.setName(request.getName());
        test.setDescription(request.getDescription());
        test.setTestId(request.getTestId());
        test.setEnvironmentId(request.getEnvironmentId());
        test.setScheduleType(request.getScheduleType());
        test.setEnabled(request.isEnabled());

        // Reset all schedule-specific fields
        test.setCronExpression(null);
        test.setFixedRateSeconds(null);
        test.setIntervalMinutes(null);
        test.setOneTimeExecutionTime(null);

        // Set schedule-specific fields based on type
        if (request.getScheduleType() == ScheduleType.CRON) {
            test.setCronExpression(request.getCronExpression());
        } else if (request.getScheduleType() == ScheduleType.FIXED_RATE) {
            test.setFixedRateSeconds(request.getFixedRateSeconds());
        } else if (request.getScheduleType() == ScheduleType.INTERVAL) {
            test.setIntervalMinutes(request.getIntervalMinutes());
        } else if (request.getScheduleType() == ScheduleType.ONE_TIME) {
            test.setOneTimeExecutionTime(request.getOneTimeExecutionTime());
        }

        test.setWebhookUrl(request.getWebhookUrl());
        
        ScheduledTest updatedTest = scheduledTestRepository.save(test);
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
        ScheduledTest test = scheduledTestRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("ScheduledTest", "id", id));
        scheduledTestRepository.delete(test);
    }

    /**
     * Execute a scheduled test manually.
     *
     * @param id the scheduled test ID
     * @param userId the user ID
     * @return the test result
     * @throws ResourceNotFoundException if the scheduled test is not found
     */
    @Transactional
    public TestResultResponse executeScheduledTest(Long id, Long userId) {
        ScheduledTest test = scheduledTestRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("ScheduledTest", "id", id));
        
        return executeTest(test);
    }

    /**
     * Scheduled task to run tests at fixed rate.
     */
    @Scheduled(fixedRate = 60000) // Run every minute
    @Transactional
    public void runScheduledTests() {
        logger.debug("Running scheduled tests check");
        
        LocalDateTime now = LocalDateTime.now();
        
        // Find tests that are due to run
        List<ScheduledTest> testsToRun = scheduledTestRepository.findByEnabledTrue().stream()
                .filter(test -> isTestDueToRun(test, now))
                .collect(Collectors.toList());
        
        logger.debug("Found {} tests to run", testsToRun.size());
        
        for (ScheduledTest test : testsToRun) {
            try {
                // Update last run time before execution
                test.setLastRunAt(now);
                test.setStatus(ScheduledTestStatus.RUNNING);
                test.setLastExecutionStartTime(now);
                scheduledTestRepository.save(test);
                
                // Execute the test
                executeTest(test);
                
                // Update after successful execution
                test.setLastExecutionEndTime(LocalDateTime.now());
                test.setLastExecutionSuccess(true);
                test.setStatus(ScheduledTestStatus.COMPLETED);
                test.incrementExecutionCount();
                
                // Calculate next run time
                updateNextRunTime(test);
                
                scheduledTestRepository.save(test);
            } catch (Exception e) {
                logger.error("Error executing scheduled test {}: {}", test.getId(), e.getMessage(), e);
                
                // Update after failed execution
                test.setLastExecutionEndTime(LocalDateTime.now());
                test.setLastExecutionSuccess(false);
                test.setLastErrorMessage(e.getMessage());
                test.setStatus(ScheduledTestStatus.FAILED);
                
                // Still calculate next run time
                updateNextRunTime(test);
                
                scheduledTestRepository.save(test);
            }
        }
    }

    /**
     * Check if a test is due to run.
     *
     * @param test the scheduled test
     * @param now the current time
     * @return true if the test is due to run
     */
    private boolean isTestDueToRun(ScheduledTest test, LocalDateTime now) {
        if (test.getScheduleType() == ScheduleType.CRON && test.getCronExpression() != null) {
            try {
                CronExpression cronExpression = CronExpression.parse(test.getCronExpression());
                LocalDateTime lastRun = test.getLastRunAt();
                
                if (lastRun == null) {
                    // If never run, check if it should run now
                    return cronExpression.next(now.minusMinutes(1)).isBefore(now);
                } else {
                    // Check if next execution time after last run is before now
                    Optional<LocalDateTime> nextAfterLast = cronExpression.next(lastRun);
                    return nextAfterLast.isPresent() && nextAfterLast.get().isBefore(now);
                }
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid cron expression for test {}: {}", test.getId(), test.getCronExpression());
                return false;
            }
        } else if (test.getScheduleType() == ScheduleType.FIXED_RATE && test.getFixedRateSeconds() != null) {
            LocalDateTime lastRun = test.getLastRunAt();
            if (lastRun == null) {
                return true; // Run immediately if never run
            }
            return lastRun.plusSeconds(test.getFixedRateSeconds()).isBefore(now);
        } else if (test.getScheduleType() == ScheduleType.INTERVAL && test.getIntervalMinutes() != null) {
            LocalDateTime lastRun = test.getLastRunAt();
            if (lastRun == null) {
                return true; // Run immediately if never run
            }
            return lastRun.plusMinutes(test.getIntervalMinutes()).isBefore(now);
        } else if (test.getScheduleType() == ScheduleType.ONE_TIME && test.getOneTimeExecutionTime() != null) {
            // For one-time, check if it's time and it hasn't run yet
            return test.getOneTimeExecutionTime().isBefore(now) && test.getLastRunAt() == null;
        }
        
        return false;
    }

    /**
     * Update the next run time for a scheduled test.
     *
     * @param test the scheduled test
     */
    private void updateNextRunTime(ScheduledTest test) {
        if (test.getScheduleType() == ScheduleType.CRON && test.getCronExpression() != null) {
            try {
                CronExpression cronExpression = CronExpression.parse(test.getCronExpression());
                LocalDateTime now = LocalDateTime.now();
                Optional<LocalDateTime> nextRun = cronExpression.next(now);
                if (nextRun.isPresent()) {
                    test.setNextRunAt(nextRun.get());
                }
            } catch (IllegalArgumentException e) {
                logger.warn("Could not parse cron expression for test {}: {}", test.getId(), test.getCronExpression());
            }
        } else if (test.getScheduleType() == ScheduleType.FIXED_RATE && test.getFixedRateSeconds() != null) {
            test.setNextRunAt(LocalDateTime.now().plusSeconds(test.getFixedRateSeconds()));
        } else if (test.getScheduleType() == ScheduleType.INTERVAL && test.getIntervalMinutes() != null) {
            test.setNextRunAt(LocalDateTime.now().plusMinutes(test.getIntervalMinutes()));
        } else if (test.getScheduleType() == ScheduleType.ONE_TIME) {
            // One-time tests don't have a next run time after execution
            test.setNextRunAt(null);
        }
    }

    /**
     * Execute a test.
     *
     * @param test the scheduled test
     * @return the test result
     */
    private TestResultResponse executeTest(ScheduledTest test) {
        logger.info("Executing scheduled test: {}", test.getId());
        
        // Execute the test
        TestResultResponse result = apiTestService.executeTest(
                test.getTestId(),
                null, // projectId - not needed for scheduled tests
                test.getUserId(),
                test.getEnvironmentId(),
                null // additionalVariables
        );
        
        // If webhook URL is provided, send the result
        if (test.getWebhookUrl() != null && !test.getWebhookUrl().isEmpty()) {
            try {
                // TODO: Implement webhook notification
                logger.info("Webhook notification would be sent to: {}", test.getWebhookUrl());
            } catch (Exception e) {
                logger.error("Error sending webhook notification: {}", e.getMessage(), e);
            }
        }
        
        return result;
    }

    /**
     * Validate a scheduled test request.
     *
     * @param request the scheduled test request
     * @throws IllegalArgumentException if the request is invalid
     */
    private void validateScheduleRequest(ScheduledTestRequest request) {
        if (request.getScheduleType() == null) {
            throw new IllegalArgumentException("Schedule type is required");
        }
        
        if (request.getScheduleType() == ScheduleType.CRON && 
                (request.getCronExpression() == null || request.getCronExpression().isEmpty())) {
            throw new IllegalArgumentException("Cron expression is required for CRON schedule type");
        }
        
        if (request.getScheduleType() == ScheduleType.FIXED_RATE && request.getFixedRateSeconds() == null) {
            throw new IllegalArgumentException("Fixed rate seconds is required for FIXED_RATE schedule type");
        }
        
        if (request.getScheduleType() == ScheduleType.INTERVAL && request.getIntervalMinutes() == null) {
            throw new IllegalArgumentException("Interval minutes is required for INTERVAL schedule type");
        }
        
        if (request.getScheduleType() == ScheduleType.ONE_TIME && request.getOneTimeExecutionTime() == null) {
            throw new IllegalArgumentException("Execution time is required for ONE_TIME schedule type");
        }
        
        if (request.getTestId() == null) {
            throw new IllegalArgumentException("Test ID is required");
        }
        
        if (request.getEnvironmentId() == null) {
            throw new IllegalArgumentException("Environment ID is required");
        }
    }

    /**
     * Map a scheduled test to a response DTO.
     *
     * @param test the scheduled test
     * @return the response DTO
     */
    private ScheduledTestResponse mapToResponse(ScheduledTest test) {
        ScheduledTestResponse response = ScheduledTestResponse.fromEntity(test);
        
        // Calculate next run time if possible
        if (test.isActive()) {
            if (test.getScheduleType() == ScheduleType.CRON && test.getCronExpression() != null) {
                try {
                    CronExpression cronExpression = CronExpression.parse(test.getCronExpression());
                    LocalDateTime now = LocalDateTime.now();
                    Optional<LocalDateTime> nextRun = cronExpression.next(now);
                    if (nextRun.isPresent()) {
                        response.setNextRunAt(nextRun.get());
                    }
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
            } else if (test.getScheduleType() == ScheduleType.FIXED_RATE && test.getFixedRateSeconds() != null) {
                LocalDateTime lastRun = test.getLastRunAt();
                if (lastRun != null) {
                    response.setNextRunAt(lastRun.plusSeconds(test.getFixedRateSeconds()));
                } else {
                    // If never run, next run is now
                    response.setNextRunAt(LocalDateTime.now());
                }
            }
        }
        
        return response;
    }
}

