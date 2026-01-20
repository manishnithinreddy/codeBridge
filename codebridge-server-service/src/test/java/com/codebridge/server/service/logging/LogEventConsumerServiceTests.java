package com.codebridge.server.service.logging;

import com.codebridge.server.dto.logging.LogEventMessage;
import com.codebridge.server.model.ServerActivityLog;
import com.codebridge.server.repository.ServerActivityLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogEventConsumerServiceTests {

    @Mock private ServerActivityLogRepository serverActivityLogRepository;
    @InjectMocks private LogEventConsumerService logEventConsumerService;

    private final int BATCH_SIZE = 5; // Test with a smaller batch size

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(logEventConsumerService, "BATCH_SIZE", BATCH_SIZE);
        // Ensure logBatch list is clean before each test if it's not re-injected
        logEventConsumerService.logBatch.clear();
    }

    @Test
    void receiveLogEvent_addsToBatch_andFlushesWhenBatchSizeReached() {
        for (int i = 0; i < BATCH_SIZE; i++) {
            LogEventMessage message = new LogEventMessage(
                UUID.randomUUID(), "ACTION_" + i, UUID.randomUUID(),
                "Details " + i, "SUCCESS", null, System.currentTimeMillis()
            );
            logEventConsumerService.receiveLogEvent(message);
        }

        ArgumentCaptor<List<ServerActivityLog>> captor = ArgumentCaptor.forClass(List.class);
        verify(serverActivityLogRepository).saveAll(captor.capture());
        assertEquals(BATCH_SIZE, captor.getValue().size());
        assertEquals(0, logEventConsumerService.logBatch.size()); // Batch should be cleared
    }

    @Test
    void receiveLogEvent_addsToBatch_doesNotFlushIfBatchSizeNotReached() {
        LogEventMessage message = new LogEventMessage(
            UUID.randomUUID(), "SINGLE_ACTION", UUID.randomUUID(),
            "Single Detail", "SUCCESS", null, System.currentTimeMillis()
        );
        logEventConsumerService.receiveLogEvent(message);

        verify(serverActivityLogRepository, never()).saveAll(any());
        assertEquals(1, logEventConsumerService.logBatch.size());
    }

    @Test
    void scheduledFlush_flushesNonEmptyBatch() {
        LogEventMessage message = new LogEventMessage(
            UUID.randomUUID(), "SCHEDULED_ACTION", UUID.randomUUID(),
            "Scheduled Detail", "SUCCESS", null, System.currentTimeMillis()
        );
        logEventConsumerService.receiveLogEvent(message); // Add one item

        assertEquals(1, logEventConsumerService.logBatch.size());

        logEventConsumerService.scheduledFlush(); // Trigger flush

        ArgumentCaptor<List<ServerActivityLog>> captor = ArgumentCaptor.forClass(List.class);
        verify(serverActivityLogRepository).saveAll(captor.capture());
        assertEquals(1, captor.getValue().size());
        assertEquals(0, logEventConsumerService.logBatch.size());
    }

    @Test
    void scheduledFlush_doesNothingForEmptyBatch() {
        logEventConsumerService.scheduledFlush();
        verify(serverActivityLogRepository, never()).saveAll(any());
    }

    @Test
    void mapToEntity_correctlyMapsMessage() {
        long timestampMillis = System.currentTimeMillis();
        UUID platformId = UUID.randomUUID();
        UUID serverId = UUID.randomUUID();
        LogEventMessage message = new LogEventMessage(
            platformId, "MAP_TEST", serverId,
            "Map Details", "PENDING", "Error XYZ", timestampMillis
        );

        ServerActivityLog entity = ReflectionTestUtils.invokeMethod(logEventConsumerService, "mapToEntity", message);

        assertNotNull(entity);
        assertEquals(platformId, entity.getPlatformUserId());
        assertEquals("MAP_TEST", entity.getAction());
        assertEquals(serverId, entity.getServerId());
        assertEquals("Map Details", entity.getDetails());
        assertEquals("PENDING", entity.getStatus());
        assertEquals("Error XYZ", entity.getErrorMessage());
        assertEquals(LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(timestampMillis), ZoneOffset.systemDefault()), entity.getTimestamp());
    }
}
