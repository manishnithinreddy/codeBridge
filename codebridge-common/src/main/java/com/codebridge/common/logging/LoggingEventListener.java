package com.codebridge.common.logging;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Listener for logging events.
 * Listens for logging events and forwards them to registered handlers.
 */
@Component
public class LoggingEventListener {

    private final List<LoggingEventHandler> handlers = new CopyOnWriteArrayList<>();

    /**
     * Registers a logging event handler.
     *
     * @param handler The handler
     */
    public void registerHandler(LoggingEventHandler handler) {
        handlers.add(handler);
    }

    /**
     * Unregisters a logging event handler.
     *
     * @param handler The handler
     * @return True if the handler was unregistered, false otherwise
     */
    public boolean unregisterHandler(LoggingEventHandler handler) {
        return handlers.remove(handler);
    }

    /**
     * Gets all registered handlers.
     *
     * @return The handlers
     */
    public List<LoggingEventHandler> getHandlers() {
        return new ArrayList<>(handlers);
    }

    /**
     * Handles a logging event.
     *
     * @param event The event
     */
    @EventListener
    public void handleEvent(LoggingEvent event) {
        for (LoggingEventHandler handler : handlers) {
            try {
                handler.handleEvent(event);
            } catch (Exception e) {
                // Log the error, but don't propagate it
                System.err.println("Error handling logging event: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Interface for logging event handlers.
     */
    public interface LoggingEventHandler {
        /**
         * Handles a logging event.
         *
         * @param event The event
         */
        void handleEvent(LoggingEvent event);
    }
}

