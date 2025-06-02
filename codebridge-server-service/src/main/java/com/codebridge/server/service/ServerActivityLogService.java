package com.codebridge.server.service;

import com.codebridge.server.dto.ServerActivityLogResponse;
import com.codebridge.server.model.Server;
import com.codebridge.server.model.ServerActivityLog;
import com.codebridge.server.dto.logging.LogEventMessage;
import com.codebridge.server.repository.ServerActivityLogRepository;
import com.codebridge.server.repository.ServerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Keep for read-only methods

import java.time.LocalDateTime; // Keep for mapToResponse if ServerActivityLog still uses it
import java.util.Optional;
import java.util.UUID;

@Service
public class ServerActivityLogService {

    private static final Logger logger = LoggerFactory.getLogger(ServerActivityLogService.class);

    private final ServerActivityLogRepository serverActivityLogRepository; // Still needed for getLogs...
    private final ServerRepository serverRepository; // Still needed for mapToResponse
    private final RabbitTemplate rabbitTemplate;

    @Value("${codebridge.rabbitmq.activity-log.exchange-name}")
    private String activityLogExchangeName;

    @Value("${codebridge.rabbitmq.activity-log.routing-key}")
    private String activityLogRoutingKey;

    public ServerActivityLogService(ServerActivityLogRepository serverActivityLogRepository,
                                    ServerRepository serverRepository,
                                    RabbitTemplate rabbitTemplate) {
        this.serverActivityLogRepository = serverActivityLogRepository;
        this.serverRepository = serverRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    // No longer @Transactional for createLog as it doesn't interact with DB directly for writes
    public void createLog(UUID platformUserId, String action, UUID serverId, String details, String status, String errorMessage) {

        LogEventMessage logEventMessage = new LogEventMessage(
                platformUserId,
                action,
                serverId,
                details,
                status,
                errorMessage,
                System.currentTimeMillis() // Using epoch milliseconds for timestamp
        );

        try {
            rabbitTemplate.convertAndSend(activityLogExchangeName, activityLogRoutingKey, logEventMessage);
            logger.info("Published activity log event to RabbitMQ: Action='{}', UserID='{}', ServerID='{}', Status='{}'",
                         action, platformUserId, serverId, status);
        } catch (Exception e) {
            // Handle RabbitMQ publishing errors, e.g., log and/or fallback strategy
            logger.error("Failed to publish activity log event to RabbitMQ: Action='{}', UserID='{}', ServerID='{}'. Error: {}",
                         action, platformUserId, serverId, e.getMessage(), e);
            // Depending on requirements, you might want to rethrow, or save to a fallback (e.g., local DB)
            // For now, just logging the error.
        }
    }

    @Transactional(readOnly = true)
    public Page<ServerActivityLogResponse> getLogsForServer(UUID serverId, Pageable pageable) {
        Page<ServerActivityLog> logs = serverActivityLogRepository.findByServerId(serverId, pageable);
        return logs.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<ServerActivityLogResponse> getLogsForUser(UUID platformUserId, Pageable pageable) {
        Page<ServerActivityLog> logs = serverActivityLogRepository.findByPlatformUserId(platformUserId, pageable);
        return logs.map(this::mapToResponse);
    }

    private ServerActivityLogResponse mapToResponse(ServerActivityLog log) {
        ServerActivityLogResponse response = new ServerActivityLogResponse();
        response.setId(log.getId());
        response.setPlatformUserId(log.getPlatformUserId());
        response.setAction(log.getAction());
        response.setDetails(log.getDetails());
        response.setStatus(log.getStatus());
        response.setErrorMessage(log.getErrorMessage());
        response.setTimestamp(log.getTimestamp());

        if (log.getServerId() != null) {
            response.setServerId(log.getServerId());
            // Fetch server name - this could be N+1 if not careful, but Pageable helps.
            // For simplicity in this example, fetching directly.
            // In a high-load scenario, consider denormalizing serverName or optimizing.
            Optional<Server> serverOpt = serverRepository.findById(log.getServerId());
            serverOpt.ifPresent(server -> response.setServerName(server.getName()));
        }
        return response;
    }
}
