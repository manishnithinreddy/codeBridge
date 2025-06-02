package com.codebridge.server.service;

import com.codebridge.server.dto.ServerRequest;
import com.codebridge.server.dto.ServerResponse;
import com.codebridge.server.exception.ResourceNotFoundException;
import com.codebridge.server.model.Server;
import com.codebridge.server.model.SshKey;
import com.codebridge.server.model.enums.ServerAuthProvider;
import com.codebridge.server.model.enums.ServerStatus;
import com.codebridge.server.repository.ServerRepository;
import com.codebridge.server.repository.SshKeyRepository; // Using SshKeyRepository directly for fetching
import org.jasypt.encryption.StringEncryptor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict; // Added
import org.springframework.cache.annotation.CachePut;   // Added
import org.springframework.cache.annotation.Cacheable;  // Added
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ServerManagementService {

    private final ServerRepository serverRepository;
    private final SshKeyRepository sshKeyRepository; // Direct repository for fetching key by ID
    private final StringEncryptor stringEncryptor;
    private final ServerActivityLogService activityLogService; // Added

    public ServerManagementService(ServerRepository serverRepository,
                                   SshKeyRepository sshKeyRepository,
                                   @Qualifier("jasyptStringEncryptor") StringEncryptor stringEncryptor,
                                   ServerActivityLogService activityLogService) { // Added
        this.serverRepository = serverRepository;
        this.sshKeyRepository = sshKeyRepository;
        this.stringEncryptor = stringEncryptor;
        this.activityLogService = activityLogService; // Added
    }

    @Transactional
    public ServerResponse createServer(ServerRequest dto, UUID userId) {
        String details;
        try {
            Server server = new Server();
            mapDtoToServer(dto, server, userId); // Helper to map and handle auth provider logic
            server.setUserId(userId);
            server.setStatus(ServerStatus.UNKNOWN); // Initial status

            Server savedServer = serverRepository.save(server);
            details = String.format("Server created: Name='%s', ID='%s', Host='%s'", savedServer.getName(), savedServer.getId(), savedServer.getHostname());
            activityLogService.createLog(userId, "SERVER_CREATE", savedServer.getId(), details, "SUCCESS", null);
            return mapToServerResponse(savedServer);
        } catch (Exception e) {
            details = String.format("Failed to create server: Name='%s', Host='%s'", dto.getName(), dto.getHostname());
            activityLogService.createLog(userId, "SERVER_CREATE_FAILED", null, details, "FAILED", e.getMessage());
            throw e;
        }
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "serverById", key = "#serverId.toString()") // Added
    public ServerResponse getServerById(UUID serverId, UUID userId) {
        Server server = serverRepository.findByIdAndUserId(serverId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Server", "id", serverId + " for user " + userId));
        return mapToServerResponse(server);
    }

    @Transactional(readOnly = true)
    public List<ServerResponse> listServersForUser(UUID userId) {
        return serverRepository.findByUserId(userId).stream()
                .map(this::mapToServerResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    @CachePut(value = "serverById", key = "#serverId.toString()") // Added
    public ServerResponse updateServer(UUID serverId, ServerRequest dto, UUID userId) {
        String details;
        try {
            Server server = serverRepository.findByIdAndUserId(serverId, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Server", "id", serverId + " for user " + userId));
            
            mapDtoToServer(dto, server, userId); // Re-use mapping logic
            // Status might be updated by a different process (e.g., connectivity check)
            // For now, direct status update from request is not included, could be added if needed.

            Server updatedServer = serverRepository.save(server);
            details = String.format("Server updated: Name='%s', ID='%s', Host='%s'", updatedServer.getName(), updatedServer.getId(), updatedServer.getHostname());
            activityLogService.createLog(userId, "SERVER_UPDATE", updatedServer.getId(), details, "SUCCESS", null);
            return mapToServerResponse(updatedServer);
        } catch (Exception e) {
            details = String.format("Failed to update server: ID='%s', Name='%s'", serverId, dto.getName());
            activityLogService.createLog(userId, "SERVER_UPDATE_FAILED", serverId, details, "FAILED", e.getMessage());
            throw e;
        }
    }

    @Transactional
    @CacheEvict(value = "serverById", key = "#serverId.toString()") // Added
    public void deleteServer(UUID serverId, UUID userId) {
        String details;
        Server server = serverRepository.findByIdAndUserId(serverId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Server", "id", serverId + " for user " + userId));
        try {
            // Add any cleanup logic here (e.g., disassociating ServerUsers, logs) if not handled by cascade.
            // For example, if ServerUser records are not cascaded, delete them here.
            // activityLogService.createLog might be called by those sub-deletions too.
            serverRepository.delete(server);
            details = String.format("Server deleted: ID='%s', Name='%s'", serverId, server.getName());
            activityLogService.createLog(userId, "SERVER_DELETE", serverId, details, "SUCCESS", null);
        } catch (Exception e) {
            details = String.format("Failed to delete server: ID='%s', Name='%s'", serverId, server.getName());
            activityLogService.createLog(userId, "SERVER_DELETE_FAILED", serverId, details, "FAILED", e.getMessage());
            throw e; // Re-throw to ensure transaction rollback if part of a larger operation
        }
    }

    private void mapDtoToServer(ServerRequest dto, Server server, UUID userId) {
        server.setName(dto.getName());
        server.setHostname(dto.getHostname());
        server.setPort(dto.getPort());
        server.setRemoteUsername(dto.getRemoteUsername());
        server.setAuthProvider(dto.getAuthProvider());
        server.setOperatingSystem(dto.getOperatingSystem());
        server.setCloudProvider(dto.getCloudProvider());

        if (dto.getAuthProvider() == ServerAuthProvider.PASSWORD) {
            if (StringUtils.hasText(dto.getPassword())) {
                server.setPassword(stringEncryptor.encrypt(dto.getPassword()));
            } else if (server.getId() == null) { // Only require password if creating new and no password set
                 throw new IllegalArgumentException("Password cannot be empty for PASSWORD authentication type.");
            }
            server.setSshKey(null); // Ensure SSH key is cleared if switching to password
        } else if (dto.getAuthProvider() == ServerAuthProvider.SSH_KEY) {
            if (dto.getSshKeyId() == null) {
                throw new IllegalArgumentException("SSH Key ID cannot be null for SSH_KEY authentication type.");
            }
            SshKey sshKey = sshKeyRepository.findById(dto.getSshKeyId())
                    .filter(key -> key.getUserId().equals(userId)) // Ensure user owns the key
                    .orElseThrow(() -> new ResourceNotFoundException("SshKey", "id", dto.getSshKeyId()));
            server.setSshKey(sshKey);
            server.setPassword(null); // Ensure password is cleared
        }
    }

    private ServerResponse mapToServerResponse(Server server) {
        if (server == null) return null;
        ServerResponse response = new ServerResponse();
        response.setId(server.getId());
        response.setName(server.getName());
        response.setHostname(server.getHostname());
        response.setPort(server.getPort());
        response.setRemoteUsername(server.getRemoteUsername());
        response.setAuthProvider(server.getAuthProvider());
        if (server.getSshKey() != null) {
            response.setSshKeyId(server.getSshKey().getId());
            response.setSshKeyName(server.getSshKey().getName());
        }
        response.setStatus(server.getStatus());
        response.setOperatingSystem(server.getOperatingSystem());
        response.setCloudProvider(server.getCloudProvider());
        response.setUserId(server.getUserId());
        response.setCreatedAt(server.getCreatedAt());
        response.setUpdatedAt(server.getUpdatedAt());
        return response;
    }

    // Internal method for services needing decrypted password
    @Transactional(readOnly = true)
    public String getDecryptedPassword(UUID serverId, UUID userId) {
        Server server = serverRepository.findByIdAndUserId(serverId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Server", "id", serverId + " for user " + userId));

        if (server.getAuthProvider() != ServerAuthProvider.PASSWORD) {
            throw new IllegalStateException("Server " + serverId + " is not configured for password authentication.");
        }
        if (!StringUtils.hasText(server.getPassword())) {
            // This case should ideally not happen if validation is correct during server setup/update
            throw new IllegalStateException("Server " + serverId + " is configured for password authentication but has no password stored.");
        }

        try {
            return stringEncryptor.decrypt(server.getPassword());
        } catch (Exception e) {
            // Log decryption failure
            // Consider specific exception handling or re-throwing as a custom exception
            throw new RuntimeException("Failed to decrypt password for server ID: " + serverId, e);
        }
    }
}
