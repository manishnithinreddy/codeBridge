package com.codebridge.common.logging;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Publisher for logging events.
 * Publishes logging events to the Spring application event system.
 */
@Component
public class LoggingEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    /**
     * Creates a new logging event publisher.
     *
     * @param eventPublisher The Spring application event publisher
     */
    @Autowired
    public LoggingEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * Publishes a logging event.
     *
     * @param event The event
     */
    public void publishEvent(LoggingEvent event) {
        eventPublisher.publishEvent(event);
    }
}

