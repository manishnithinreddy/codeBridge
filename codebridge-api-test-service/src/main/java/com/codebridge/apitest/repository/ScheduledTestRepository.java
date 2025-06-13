package com.codebridge.apitest.repository;

import com.codebridge.apitest.model.ScheduledTest;
import com.codebridge.apitest.model.enums.ScheduleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for scheduled tests.
 */
@Repository
public interface ScheduledTestRepository extends JpaRepository<ScheduledTest, UUID> {
    
    /**
     * Find all scheduled tests for a user.
     *
     * @param userId the user ID
     * @return the list of scheduled tests
     */
    List<ScheduledTest> findByUserId(UUID userId);
    
    /**
     * Find a scheduled test by ID and user ID.
     *
     * @param id the scheduled test ID
     * @param userId the user ID
     * @return the scheduled test
     */
    Optional<ScheduledTest> findByIdAndUserId(UUID id, UUID userId);
    
    /**
     * Find all active scheduled tests.
     *
     * @return the list of active scheduled tests
     */
    List<ScheduledTest> findByActiveTrue();
    
    /**
     * Find all active scheduled tests with a specific schedule type.
     *
     * @param scheduleType the schedule type
     * @return the list of active scheduled tests with the specified schedule type
     */
    List<ScheduledTest> findByScheduleTypeAndActiveTrue(ScheduleType scheduleType);
}

