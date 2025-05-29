package com.codebridge.server.service;

import com.codebridge.server.dto.ServerActivityLogResponse;
import com.codebridge.server.model.Server;
import com.codebridge.server.model.ServerActivityLog;
import com.codebridge.server.repository.ServerActivityLogRepository;
import com.codebridge.server.repository.ServerRepository; // Needed to fetch server for serverName
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class ServerActivityLogService {

    private final ServerActivityLogRepository serverActivityLogRepository;
    private final ServerRepository serverRepository; // To fetch server name for response

    public ServerActivityLogService(ServerActivityLogRepository serverActivityLogRepository,
                                    ServerRepository serverRepository) {
        this.serverActivityLogRepository = serverActivityLogRepository;
        this.serverRepository = serverRepository;
    }

    @Transactional
    public void createLog(UUID platformUserId, String action, UUID serverId, String details, String status, String errorMessage) {
        ServerActivityLog log = new ServerActivityLog();
        log.setPlatformUserId(platformUserId);
        log.setAction(action);
        log.setDetails(details);
        log.setStatus(status);
        log.setErrorMessage(errorMessage);
        log.setTimestamp(LocalDateTime.now());

        if (serverId != null) {
            // Optionally, you could fetch the Server entity if you need to ensure it exists
            // or store a direct reference, but for logging, serverId might be enough.
            // For this implementation, we'll assume serverId is sufficient for the log record itself,
            // and serverName for the response DTO will be fetched on demand.
            Server serverRef = new Server(); // Create a reference proxy
            serverRef.setId(serverId);
            log.setServer(serverRef);
        }

        serverActivityLogRepository.save(log);
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

        if (log.getServer() != null && log.getServer().getId() != null) {
            response.setServerId(log.getServer().getId());
            // Fetch server name - this could be N+1 if not careful, but Pageable helps.
            // For simplicity in this example, fetching directly.
            // In a high-load scenario, consider denormalizing serverName or optimizing.
            Optional<Server> serverOpt = serverRepository.findById(log.getServer().getId());
            serverOpt.ifPresent(server -> response.setServerName(server.getName()));
        }
        return response;
    }
}
