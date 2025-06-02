package com.codebridge.server.service;

import com.codebridge.server.dto.ServerRequest;
import com.codebridge.server.dto.ServerResponse;
import com.codebridge.server.exception.ResourceNotFoundException;
import com.codebridge.server.model.Server;
import com.codebridge.server.model.SshKey;
import com.codebridge.server.model.enums.ServerAuthProvider;
import com.codebridge.server.model.enums.ServerCloudProvider;
import com.codebridge.server.model.enums.ServerStatus;
import com.codebridge.server.repository.ServerRepository;
import com.codebridge.server.repository.SshKeyRepository; // Or SshKeyManagementService
import org.jasypt.encryption.StringEncryptor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ServerManagementService {

    private final ServerRepository serverRepository;
    private final SshKeyRepository sshKeyRepository; // Using SshKeyRepository for fetching SshKey entity
    private final StringEncryptor stringEncryptor;

    public ServerManagementService(ServerRepository serverRepository,
                                   SshKeyRepository sshKeyRepository,
                                   @Qualifier("jasyptStringEncryptor") StringEncryptor stringEncryptor) {
        this.serverRepository = serverRepository;
        this.sshKeyRepository = sshKeyRepository;
        this.stringEncryptor = stringEncryptor;
    }

    @Transactional
    public ServerResponse createServer(ServerRequest requestDto, UUID userId) {
        Server server = new Server();
        server.setName(requestDto.getName());
        server.setHostname(requestDto.getHostname());
        server.setPort(requestDto.getPort() != null ? requestDto.getPort() : 22);
        server.setRemoteUsername(requestDto.getRemoteUsername());
        server.setUserId(userId);
        server.setStatus(ServerStatus.UNKNOWN); // Initial status

        try {
            server.setAuthProvider(ServerAuthProvider.valueOf(requestDto.getAuthProvider().toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid authProvider value: " + requestDto.getAuthProvider());
        }

        if (server.getAuthProvider() == ServerAuthProvider.PASSWORD) {
            if (requestDto.getPassword() == null || requestDto.getPassword().isBlank()) {
                throw new IllegalArgumentException("Password cannot be blank for PASSWORD auth provider.");
            }
            server.setPassword(stringEncryptor.encrypt(requestDto.getPassword()));
        } else if (server.getAuthProvider() == ServerAuthProvider.SSH_KEY) {
            if (requestDto.getSshKeyId() == null) {
                throw new IllegalArgumentException("sshKeyId cannot be null for SSH_KEY auth provider.");
            }
            SshKey sshKey = sshKeyRepository.findByIdAndUserId(requestDto.getSshKeyId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("SshKey", "id", requestDto.getSshKeyId()));
            server.setSshKey(sshKey);
        }

        if (requestDto.getCloudProvider() != null && !requestDto.getCloudProvider().isBlank()) {
            try {
                server.setCloudProvider(ServerCloudProvider.valueOf(requestDto.getCloudProvider().toUpperCase()));
            } catch (IllegalArgumentException e) {
                 // Log warning or handle as per requirements, e.g., set to OTHER or throw error
                server.setCloudProvider(ServerCloudProvider.OTHER);
            }
        }
        server.setOperatingSystem(requestDto.getOperatingSystem());

        Server savedServer = serverRepository.save(server);
        return mapToServerResponse(savedServer);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "serverById", key = "#serverId.toString()")
    public ServerResponse getServerById(UUID serverId, UUID userId) {
        Server server = serverRepository.findByIdAndUserId(serverId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Server", "id", serverId));
        return mapToServerResponse(server);
    }

    @Transactional(readOnly = true)
    public List<ServerResponse> listServersForUser(UUID userId) {
        return serverRepository.findByUserId(userId).stream()
                .map(this::mapToServerResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    @CachePut(value = "serverById", key = "#serverId.toString()")
    public ServerResponse updateServer(UUID serverId, ServerRequest requestDto, UUID userId) {
        Server server = serverRepository.findByIdAndUserId(serverId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Server", "id", serverId));

        server.setName(requestDto.getName());
        server.setHostname(requestDto.getHostname());
        server.setPort(requestDto.getPort() != null ? requestDto.getPort() : 22);
        server.setRemoteUsername(requestDto.getRemoteUsername());

        // Auth provider and credentials update logic
        if (requestDto.getAuthProvider() != null) {
            ServerAuthProvider newAuthProvider;
            try {
                newAuthProvider = ServerAuthProvider.valueOf(requestDto.getAuthProvider().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid authProvider value: " + requestDto.getAuthProvider());
            }
            server.setAuthProvider(newAuthProvider);

            if (newAuthProvider == ServerAuthProvider.PASSWORD) {
                if (requestDto.getPassword() != null && !requestDto.getPassword().isBlank()) { // Allow updating password
                    server.setPassword(stringEncryptor.encrypt(requestDto.getPassword()));
                    server.setSshKey(null); // Remove SSH key if switching to password
                } else if (server.getPassword() == null) { // If it's a new switch to password and no password provided
                     throw new IllegalArgumentException("Password cannot be blank when auth provider is PASSWORD.");
                }
                // If password is not provided in update, keep the old one (assuming it's already set)
            } else if (newAuthProvider == ServerAuthProvider.SSH_KEY) {
                if (requestDto.getSshKeyId() != null) {
                    SshKey sshKey = sshKeyRepository.findByIdAndUserId(requestDto.getSshKeyId(), userId)
                            .orElseThrow(() -> new ResourceNotFoundException("SshKey", "id", requestDto.getSshKeyId()));
                    server.setSshKey(sshKey);
                    server.setPassword(null); // Remove password if switching to SSH key
                } else if (server.getSshKey() == null) { // If it's a new switch to SSH and no key ID provided
                    throw new IllegalArgumentException("sshKeyId cannot be null when auth provider is SSH_KEY.");
                }
                // If sshKeyId is not provided in update, keep the old one (assuming it's already set)
            }
        }


        if (requestDto.getCloudProvider() != null && !requestDto.getCloudProvider().isBlank()) {
             try {
                server.setCloudProvider(ServerCloudProvider.valueOf(requestDto.getCloudProvider().toUpperCase()));
            } catch (IllegalArgumentException e) {
                server.setCloudProvider(ServerCloudProvider.OTHER);
            }
        } else {
            server.setCloudProvider(null); // Allow clearing cloud provider
        }
        server.setOperatingSystem(requestDto.getOperatingSystem()); // Allow clearing OS

        Server updatedServer = serverRepository.save(server);
        return mapToServerResponse(updatedServer);
    }

    @Transactional
    @CacheEvict(value = "serverById", key = "#serverId.toString()")
    @CacheEvict(value="userServerAccessDetails", allEntries=true) // Added for broader eviction
    public void deleteServer(UUID serverId, UUID userId) {
        Server server = serverRepository.findByIdAndUserId(serverId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Server", "id", serverId));
        // Consider implications: e.g., linked ServerUser entries, ServerActivityLog entries
        // Depending on cascading rules or business logic, these might need to be handled.
        serverRepository.delete(server);
    }

    private ServerResponse mapToServerResponse(Server server) {
        ServerResponse response = new ServerResponse();
        response.setId(server.getId());
        response.setName(server.getName());
        response.setHostname(server.getHostname());
        response.setPort(server.getPort());
        response.setRemoteUsername(server.getRemoteUsername());
        if (server.getAuthProvider() != null) {
            response.setAuthProvider(server.getAuthProvider().name());
        }
        if (server.getSshKey() != null) {
            response.setSshKeyId(server.getSshKey().getId());
        }
        if (server.getStatus() != null) {
            response.setStatus(server.getStatus().name());
        }
        response.setOperatingSystem(server.getOperatingSystem());
        if (server.getCloudProvider() != null) {
            response.setCloudProvider(server.getCloudProvider().name());
        }
        response.setUserId(server.getUserId());
        response.setCreatedAt(server.getCreatedAt());
        response.setUpdatedAt(server.getUpdatedAt());
        return response;
    }
}
