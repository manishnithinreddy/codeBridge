package com.codebridge.server.service;

import com.codebridge.core.util.RequestUtils;
import com.codebridge.server.dto.ServerActivityLogResponse;
import com.codebridge.server.model.Server;
import com.codebridge.server.model.ServerActivityLog;
import com.codebridge.server.repository.ServerActivityLogRepository;
import com.codebridge.server.repository.ServerRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import com.codebridge.server.dto.logging.LogEventMessage; // Added
import org.springframework.amqp.rabbit.core.RabbitTemplate; // Added
import org.springframework.beans.factory.annotation.Value; // Added
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Still needed for read methods
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;
import java.util.UUID;

@Service
public class ServerActivityLogService {

    private static final Logger logger = LoggerFactory.getLogger(ServerActivityLogService.class);

    private final ServerActivityLogRepository serverActivityLogRepository; // Still needed for read operations
    private final ServerRepository serverRepository; // To fetch server name for responses
    private final RabbitTemplate rabbitTemplate; // Added

    @Value("${codebridge.rabbitmq.activity-log.exchange-name}")
    private String activityLogExchangeName; // Added

    @Value("${codebridge.rabbitmq.activity-log.routing-key}")
    private String activityLogRoutingKey; // Added

    public ServerActivityLogService(ServerActivityLogRepository serverActivityLogRepository,
                                    ServerRepository serverRepository,
                                    RabbitTemplate rabbitTemplate) { // Added rabbitTemplate
        this.serverActivityLogRepository = serverActivityLogRepository;
        this.serverRepository = serverRepository;
        this.rabbitTemplate = rabbitTemplate; // Added
    }

    // No longer directly transactional for DB write; publishing is fire-and-forget or handled by AMQP transactions if configured
    public void createLog(UUID platformUserId, String action, UUID serverId, String details, String status, String errorMessage) {
        try {
            // Get the current HTTP request if available
            HttpServletRequest request = getCurrentRequest();
            String ipAddress = "unknown";
            String userAgent = "unknown";
            
            if (request != null) {
                ipAddress = RequestUtils.getClientIpAddress(request);
                userAgent = RequestUtils.getUserAgent(request);
            }
            
            LogEventMessage logEventMessage = new LogEventMessage(
                    platformUserId,
                    action,
                    serverId, // Can be null
                    details,
                    status,
                    errorMessage, // Can be null
                    ipAddress, // Add IP address
                    userAgent, // Add user agent
                    System.currentTimeMillis() // Epoch millis
            );

            rabbitTemplate.convertAndSend(activityLogExchangeName, activityLogRoutingKey, logEventMessage);
            logger.info("Activity log event published: User: {}, Action: {}, ServerID: {}, IP: {}", 
                       platformUserId, action, serverId, ipAddress);

        } catch (Exception e) {
            // Log to system logger if publishing fails
            logger.error("Failed to publish activity log event to RabbitMQ. User: {}, Action: {}, ServerID: {}, Details: {}, Status: {}, Error: {}",
                         platformUserId, action, serverId, details, status, errorMessage, e.getMessage(), e);
            // Depending on policy, might re-throw or try a fallback (like direct DB write if critical)
        }
    }

    @Transactional(readOnly = true)
    public Page<ServerActivityLogResponse> getLogsForServer(UUID serverId, Pageable pageable) {
        return serverActivityLogRepository.findByServerId(serverId, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<ServerActivityLogResponse> getLogsForUser(UUID platformUserId, Pageable pageable) {
        // This fetches logs initiated BY the user, not necessarily logs ABOUT a user if they were an object of an action.
        return serverActivityLogRepository.findByPlatformUserId(platformUserId, pageable)
                .map(this::mapToResponse);
    }

    private ServerActivityLogResponse mapToResponse(ServerActivityLog log) {
        if (log == null) return null;

        String serverName = null;
        if (log.getServerId() != null) {
            Optional<Server> serverOpt = serverRepository.findById(log.getServerId());
            serverName = serverOpt.map(Server::getName).orElse(null);
        }

        // Assuming platformUsername would be fetched from a UserService if available, for now null
        return new ServerActivityLogResponse(
                log.getId(),
                log.getServerId(),
                serverName,
                log.getPlatformUserId(),
                null, // platformUsername placeholder
                log.getAction(),
                log.getDetails(),
                log.getStatus(),
                log.getErrorMessage(),
                log.getIpAddress(), // Add IP address
                log.getUserAgent(), // Add user agent
                log.getTimestamp()
        );
    }

    /**
     * Get the current HTTP request.
     *
     * @return the current HTTP request, or null if not available
     */
    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
}
