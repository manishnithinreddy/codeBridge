package com.codebridge.apitest.model.enums;

/**
 * Enum for schedule types.
 */
public enum ScheduleType {
    /**
     * Cron-based schedule.
     */
    CRON,
    
    /**
     * Fixed rate schedule.
     */
    FIXED_RATE,
    
    /**
     * Interval-based schedule.
     */
    INTERVAL,
    
    /**
     * One-time execution.
     */
    ONE_TIME
}

