package com.codebridge.server.service;

import com.codebridge.server.dto.ServerActivityLogResponse;
import com.codebridge.server.dto.logging.LogEventMessage;
import com.codebridge.server.model.Server;
import com.codebridge.server.model.ServerActivityLog;
import com.codebridge.server.repository.ServerActivityLogRepository;
import com.codebridge.server.repository.ServerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;
import org.springframework.test.util.ReflectionTestUtils;


@ExtendWith(MockitoExtension.class)
class ServerActivityLogServiceTests {

    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private ServerActivityLogRepository serverActivityLogRepository;
    @Mock private ServerRepository serverRepository;

    @InjectMocks
    private ServerActivityLogService serverActivityLogService;

    private final String exchangeName = "test.exchange";
    private final String routingKey = "test.routingkey";

    @Test
    void createLog_publishesToRabbitMQ() {
        ReflectionTestUtils.setField(serverActivityLogService, "activityLogExchangeName", exchangeName);
        ReflectionTestUtils.setField(serverActivityLogService, "activityLogRoutingKey", routingKey);

        UUID platformUserId = UUID.randomUUID();
        UUID serverId = UUID.randomUUID();
        String action = "TEST_ACTION";
        String details = "Test details";
        String status = "SUCCESS";

        serverActivityLogService.createLog(platformUserId, action, serverId, details, status, null);

        ArgumentCaptor<LogEventMessage> captor = ArgumentCaptor.forClass(LogEventMessage.class);
        verify(rabbitTemplate).convertAndSend(eq(exchangeName), eq(routingKey), captor.capture());

        LogEventMessage publishedMessage = captor.getValue();
        assertEquals(platformUserId, publishedMessage.platformUserId());
        assertEquals(action, publishedMessage.action());
        assertEquals(serverId, publishedMessage.serverId());
        assertEquals(details, publishedMessage.details());
        assertEquals(status, publishedMessage.status());
    }

    @Test
    void getLogsForServer_returnsPagedResponse() {
        UUID serverId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        ServerActivityLog log = new ServerActivityLog();
        log.setId(UUID.randomUUID());
        log.setServerId(serverId);
        log.setAction("SERVER_START");
        log.setPlatformUserId(UUID.randomUUID());
        log.setStatus("SUCCESS");
        log.setTimestamp(LocalDateTime.now());

        Server server = new Server();
        server.setId(serverId);
        server.setName("TestServer");

        Page<ServerActivityLog> page = new PageImpl<>(Collections.singletonList(log), pageable, 1);
        when(serverActivityLogRepository.findByServerId(serverId, pageable)).thenReturn(page);
        when(serverRepository.findById(serverId)).thenReturn(Optional.of(server));

        Page<ServerActivityLogResponse> responsePage = serverActivityLogService.getLogsForServer(serverId, pageable);

        assertNotNull(responsePage);
        assertEquals(1, responsePage.getTotalElements());
        assertEquals("TestServer", responsePage.getContent().get(0).getServerName());
        verify(serverActivityLogRepository).findByServerId(serverId, pageable);
    }
}
