package com.codebridge.server.service.logging;

import com.codebridge.server.dto.logging.LogEventMessage;
import com.codebridge.server.model.ServerActivityLog;
import com.codebridge.server.repository.ServerActivityLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.test.util.ReflectionTestUtils;


import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogEventConsumerServiceTests {

    @Mock
    private ServerActivityLogRepository serverActivityLogRepositoryMock;

    @InjectMocks
    private LogEventConsumerService logEventConsumerService;

    @Captor
    private ArgumentCaptor<List<ServerActivityLog>> logBatchCaptor;

    private final int BATCH_SIZE = 100; // Match the constant in the service

    @BeforeEach
    void setUp() {
        // If BATCH_SIZE or BATCH_TIMEOUT_MS were configurable via @Value, set them here.
        // Since they are static final, we use the same value.
        // Reset internal batch list and lastFlushTime for each test
        ReflectionTestUtils.setField(logEventConsumerService, "logBatch", new java.util.ArrayList<>());
        ReflectionTestUtils.setField(logEventConsumerService, "lastFlushTime", System.currentTimeMillis());
    }

    private LogEventMessage createSampleLogEventMessage(UUID userId, String action, UUID serverId) {
        return new LogEventMessage(userId, action, serverId, "details", "SUCCESS", null, System.currentTimeMillis());
    }

    @Test
    void receiveLogEvent_singleMessage_doesNotFlushIfBatchSizeNotReached() {
        LogEventMessage message = createSampleLogEventMessage(UUID.randomUUID(), "ACTION_1", null);
        logEventConsumerService.receiveLogEvent(message);

        // Verify batch contains one item (cannot directly access private list without reflection/getter)
        // So, verify saveAll is NOT called
        verify(serverActivityLogRepositoryMock, never()).saveAll(any());
    }

    @Test
    void receiveLogEvent_reachesBatchSize_flushesBatch() {
        UUID userId = UUID.randomUUID();
        for (int i = 0; i < BATCH_SIZE; i++) {
            logEventConsumerService.receiveLogEvent(createSampleLogEventMessage(userId, "ACTION_" + i, null));
        }

        verify(serverActivityLogRepositoryMock, times(1)).saveAll(logBatchCaptor.capture());
        List<ServerActivityLog> capturedBatch = logBatchCaptor.getValue();
        assertEquals(BATCH_SIZE, capturedBatch.size());
        assertEquals("ACTION_0", capturedBatch.get(0).getAction());
        assertEquals("ACTION_" + (BATCH_SIZE - 1), capturedBatch.get(capturedBatch.size()-1).getAction());

        // Verify batch is cleared (by trying to flush again via scheduler, should not save)
        logEventConsumerService.scheduledFlush();
        verify(serverActivityLogRepositoryMock, times(1)).saveAll(any()); // Still only 1 time
    }

    @Test
    void receiveLogEvent_messageConversion_correctlyMapsFields() {
        UUID userId = UUID.randomUUID();
        UUID serverId = UUID.randomUUID();
        String action = "MAP_TEST";
        String details = "Mapping details";
        String status = "FAILURE";
        String errorMessage = "An error occurred";
        long timestampMillis = System.currentTimeMillis();

        LogEventMessage message = new LogEventMessage(userId, action, serverId, details, status, errorMessage, timestampMillis);

        // Trigger a flush to capture the entity
        ReflectionTestUtils.setField(logEventConsumerService, "BATCH_SIZE", 1); // Force flush
        logEventConsumerService.receiveLogEvent(message);


        verify(serverActivityLogRepositoryMock).saveAll(logBatchCaptor.capture());
        ServerActivityLog capturedLog = logBatchCaptor.getValue().get(0);

        assertEquals(userId, capturedLog.getPlatformUserId());
        assertEquals(action, capturedLog.getAction());
        assertEquals(serverId, capturedLog.getServerId());
        assertEquals(details, capturedLog.getDetails());
        assertEquals(status, capturedLog.getStatus());
        assertEquals(errorMessage, capturedLog.getErrorMessage());
        assertNotNull(capturedLog.getTimestamp());

        LocalDateTime expectedTimestamp = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestampMillis), ZoneId.systemDefault());
        // Allow for a tiny difference due to conversion precision if necessary, though should be exact
        assertTrue(expectedTimestamp.isEqual(capturedLog.getTimestamp()) ||
                   expectedTimestamp.minusNanos(1000000).isBefore(capturedLog.getTimestamp()) && expectedTimestamp.plusNanos(1000000).isAfter(capturedLog.getTimestamp()),
                   "Timestamp mismatch. Expected: " + expectedTimestamp + ", Actual: " + capturedLog.getTimestamp());


    }

    @Test
    void receiveLogEvent_nullMessage_isDiscarded() {
        logEventConsumerService.receiveLogEvent(null);
        verify(serverActivityLogRepositoryMock, never()).saveAll(any());
        // Check logs if specific warning is expected (requires Logback appender setup for tests)
    }


    @Test
    void scheduledFlush_flushesPartialBatch() {
        // Add less than BATCH_SIZE items
        for (int i = 0; i < BATCH_SIZE / 2; i++) {
            logEventConsumerService.receiveLogEvent(createSampleLogEventMessage(UUID.randomUUID(), "PARTIAL_" + i, null));
        }

        // Verify not flushed yet by size
        verify(serverActivityLogRepositoryMock, never()).saveAll(any());

        logEventConsumerService.scheduledFlush(); // Trigger time-based flush

        verify(serverActivityLogRepositoryMock, times(1)).saveAll(logBatchCaptor.capture());
        assertEquals(BATCH_SIZE / 2, logBatchCaptor.getValue().size());

        // Verify batch is cleared
        logEventConsumerService.scheduledFlush();
        verify(serverActivityLogRepositoryMock, times(1)).saveAll(any()); // Still only 1 time
    }

    @Test
    void scheduledFlush_emptyBatch_doesNotCallSaveAll() {
        // Ensure batch is empty (should be after setUp)
        logEventConsumerService.scheduledFlush();
        verify(serverActivityLogRepositoryMock, never()).saveAll(any());
    }

    @Test
    void flushLogBatch_dataAccessExceptionOnSaveAll_logsErrorAndClearsBatch() {
        // Add some items
        logEventConsumerService.receiveLogEvent(createSampleLogEventMessage(UUID.randomUUID(), "FAIL_ACTION", null));

        // Mock saveAll to throw exception
        doThrow(new DataAccessException("Test DB error") {}).when(serverActivityLogRepositoryMock).saveAll(anyList());

        // Use reflection to check internal batch before flush, or trigger flush
        // We'll trigger flush via scheduledFlush for simplicity
        logEventConsumerService.scheduledFlush();

        // Verify saveAll was attempted
        verify(serverActivityLogRepositoryMock, times(1)).saveAll(anyList());

        // Verify batch is cleared even after error (as per current implementation)
        logEventConsumerService.scheduledFlush(); // Try to flush again
        verify(serverActivityLogRepositoryMock, times(1)).saveAll(anyList()); // Still only 1 attempt, meaning batch was cleared

        // TODO: Add log capture to verify the error was logged.
    }
}
