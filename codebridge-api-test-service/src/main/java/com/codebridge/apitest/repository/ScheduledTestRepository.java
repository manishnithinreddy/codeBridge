package com.codebridge.apitest.repository;

import com.codebridge.apitest.model.ScheduledTest;
import com.codebridge.apitest.model.enums.ScheduleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for scheduled tests.
 */
@Repository
public interface ScheduledTestRepository extends JpaRepository<ScheduledTest, Long> {
    
    /**
     * Find all scheduled tests for a user.
     *
     * @param userId the user ID
     * @return the list of scheduled tests
     */
    List<ScheduledTest> findByUserId(Long userId);
    
    /**
     * Find a scheduled test by ID and user ID.
     *
     * @param id the scheduled test ID
     * @param userId the user ID
     * @return the scheduled test
     */
    Optional<ScheduledTest> findByIdAndUserId(Long id, Long userId);
    
    /**
     * Find all active scheduled tests.
     *
     * @return the list of active scheduled tests
     */
    List<ScheduledTest> findByActiveTrue();
    
    /**
     * Find all enabled scheduled tests.
     *
     * @return the list of enabled scheduled tests
     */
    List<ScheduledTest> findByEnabledTrue();
    
    /**
     * Find all active scheduled tests with a specific schedule type.
     *
     * @param scheduleType the schedule type
     * @return the list of active scheduled tests with the specified schedule type
     */
    List<ScheduledTest> findByScheduleTypeAndActiveTrue(ScheduleType scheduleType);
}

