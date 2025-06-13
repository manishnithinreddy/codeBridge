package com.codebridge.apitest.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Configuration for task scheduling.
 */
@Configuration
@EnableScheduling
public class SchedulerConfig {
    
    /**
     * Creates a task scheduler with a thread pool.
     *
     * @return the task scheduler
     */
    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10);
        scheduler.setThreadNamePrefix("scheduled-task-");
        scheduler.setAwaitTerminationSeconds(60);
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setErrorHandler(t -> {
            // Log the error but don't rethrow it
            if (t instanceof Exception) {
                Exception e = (Exception) t;
                // Log the exception
                System.err.println("Error in scheduled task: " + e.getMessage());
                e.printStackTrace();
            }
        });
        return scheduler;
    }
}

