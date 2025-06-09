package com.codebridge.server.service.logging;

import com.codebridge.server.dto.logging.LogEventMessage;
import com.codebridge.server.model.ServerActivityLog;
import com.codebridge.server.repository.ServerActivityLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DataAccessException;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class LogEventConsumerService {

    private static final Logger logger = LoggerFactory.getLogger(LogEventConsumerService.class);

    private static final int BATCH_SIZE = 100; // Configurable via @Value if needed
    // Using fixedDelay for simplicity, ensure this value is less than message visibility timeout if applicable
    private static final long BATCH_FLUSH_FIXED_DELAY_MS = 5000; // 5 seconds
    private static final long BATCH_FLUSH_INITIAL_DELAY_MS = 5000; // 5 seconds


    private final ServerActivityLogRepository serverActivityLogRepository;
    private final List<ServerActivityLog> logBatch = new ArrayList<>(BATCH_SIZE);

    public LogEventConsumerService(ServerActivityLogRepository serverActivityLogRepository) {
        this.serverActivityLogRepository = serverActivityLogRepository;
    }

    @RabbitListener(queues = "${codebridge.rabbitmq.activity-log.queue-name}")
    public void receiveLogEvent(@Payload LogEventMessage logEventMessage) {
        logger.debug("Received log event: {}", logEventMessage);
        try {
            ServerActivityLog logEntity = mapToEntity(logEventMessage);

            synchronized (logBatch) {
                logBatch.add(logEntity);
                if (logBatch.size() >= BATCH_SIZE) {
                    logger.info("Log batch reached size {}, flushing...", BATCH_SIZE);
                    flushLogBatchInternal(); // Call internal method that is synchronized
                }
            }
        } catch (Exception e) {
            logger.error("Error processing received log event: {}. Event: {}", e.getMessage(), logEventMessage, e);
            // Depending on error handling strategy, message might be requeued or sent to DLQ
            // For now, just logging the error.
        }
    }

    @Scheduled(fixedDelay = BATCH_FLUSH_FIXED_DELAY_MS, initialDelay = BATCH_FLUSH_INITIAL_DELAY_MS)
    public void scheduledFlush() {
        logger.debug("Scheduled log batch flush triggered.");
        flushLogBatch();
    }

    // Public synchronized method for explicit flush if needed, or for @Scheduled
    public void flushLogBatch() {
        synchronized (logBatch) {
            flushLogBatchInternal();
        }
    }

    // Internal method to be called from synchronized blocks
    @Transactional // Apply transaction to the batch save operation
    private void flushLogBatchInternal() {
        if (logBatch.isEmpty()) {
            logger.debug("Log batch is empty, nothing to flush.");
            return;
        }

        List<ServerActivityLog> batchToSave = new ArrayList<>(logBatch); // Copy to avoid ConcurrentModification if save is slow
        logBatch.clear();

        logger.info("Flushing {} log events to database.", batchToSave.size());
        try {
            serverActivityLogRepository.saveAll(batchToSave);
            logger.info("Successfully saved {} log events to database.", batchToSave.size());
        } catch (DataAccessException e) {
            logger.error("Error saving log batch to database: {}", e.getMessage(), e);
            // Handle failed batch: e.g., retry, log to a dead-letter file, etc.
            // For now, logging the error. These logs might be lost if not handled.
            // Re-add to batch could cause loop if DB is down; consider a DLQ strategy or limited retries.
            // For simplicity here, items are cleared.
        } catch (Exception e) {
            logger.error("Unexpected error during log batch save: {}", e.getMessage(), e);
        }
    }

    private ServerActivityLog mapToEntity(LogEventMessage dto) {
        ServerActivityLog entity = new ServerActivityLog();
        entity.setPlatformUserId(dto.platformUserId());
        entity.setAction(dto.action());
        entity.setServerId(dto.serverId()); // Can be null
        entity.setDetails(dto.details());
        entity.setStatus(dto.status());
        entity.setErrorMessage(dto.errorMessage()); // Can be null
        entity.setIpAddress(dto.ipAddress()); // Set IP address
        entity.setUserAgent(dto.userAgent()); // Set user agent
        entity.setTimestamp(LocalDateTime.ofInstant(Instant.ofEpochMilli(dto.timestamp()), ZoneId.systemDefault()));
        return entity;
    }
}
