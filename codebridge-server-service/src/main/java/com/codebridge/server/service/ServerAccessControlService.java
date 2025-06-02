package com.codebridge.server.service;

import com.codebridge.server.dto.ServerUserRequest;
import com.codebridge.server.dto.ServerUserResponse;
import com.codebridge.server.dto.UserSpecificConnectionDetailsDto;
import com.codebridge.server.exception.AccessDeniedException;
import com.codebridge.server.exception.ResourceNotFoundException;
import com.codebridge.server.model.Server;
import com.codebridge.server.model.ServerUser;
import com.codebridge.server.model.SshKey;
import com.codebridge.server.model.enums.ServerAuthProvider;
import com.codebridge.server.repository.ServerRepository;
import com.codebridge.server.repository.ServerUserRepository;
import com.codebridge.server.repository.SshKeyRepository;
import org.springframework.cache.annotation.CacheEvict; // Added
import org.springframework.cache.annotation.Cacheable;  // Added
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class ServerAccessControlService {

    private final ServerUserRepository serverUserRepository;
    private final ServerRepository serverRepository;
    private final SshKeyRepository sshKeyRepository;
    private final SshKeyManagementService sshKeyManagementService; // For decrypting user-specific key
    private final ServerManagementService serverManagementService; // Added for owner password decryption
    private final ServerActivityLogService activityLogService; // Added

    public ServerAccessControlService(ServerUserRepository serverUserRepository,
                                      ServerRepository serverRepository,
                                      SshKeyRepository sshKeyRepository,
                                      SshKeyManagementService sshKeyManagementService,
                                      ServerManagementService serverManagementService,
                                      ServerActivityLogService activityLogService) { // Added
        this.serverUserRepository = serverUserRepository;
        this.serverRepository = serverRepository;
        this.sshKeyRepository = sshKeyRepository;
        this.sshKeyManagementService = sshKeyManagementService;
        this.serverManagementService = serverManagementService;
        this.activityLogService = activityLogService; // Added
    }

    @Transactional
    @CacheEvict(value = "userServerAccessDetails", key = "#requestDto.platformUserId.toString() + ':' + #serverId.toString()") // Added
    public ServerUserResponse grantServerAccess(UUID adminUserId, UUID serverId, ServerUserRequest requestDto) {
        String details;
        String logStatus = "FAILED";
        ServerUser savedServerUser = null;

        try {
            Server server = serverRepository.findById(serverId)
                    .orElseThrow(() -> new ResourceNotFoundException("Server", "id", serverId));

            // Verify adminUserId owns the server
        if (!server.getUserId().equals(adminUserId)) {
            throw new AccessDeniedException("User " + adminUserId + " does not own server " + serverId + ". Cannot grant access.");
        }
        
        if (adminUserId.equals(requestDto.getPlatformUserId())) {
            throw new IllegalArgumentException("Cannot grant server access to the server owner via ServerUser record. Owner has implicit access.");
        }

        SshKey sshKeyForUser = null;
        if (requestDto.getSshKeyIdForUser() != null) {
            // Ensure the adminUserId (who is granting access) also owns the key being assigned.
            // Or, if keys can be public/shared, this check might differ. For now, assume admin owns assigned key.
            sshKeyForUser = sshKeyRepository.findById(requestDto.getSshKeyIdForUser())
                    // .filter(key -> key.getUserId().equals(adminUserId)) // This check might be too restrictive if admin is assigning a user's own key or a shared key
                    .orElseThrow(() -> new ResourceNotFoundException("SshKey", "id", requestDto.getSshKeyIdForUser()));
        }

        Optional<ServerUser> existingGrantOpt = serverUserRepository.findByServerIdAndPlatformUserId(serverId, requestDto.getPlatformUserId());
        
        ServerUser serverUser = existingGrantOpt.orElse(new ServerUser());
        serverUser.setServer(server);
        serverUser.setPlatformUserId(requestDto.getPlatformUserId());
        serverUser.setRemoteUsernameForUser(requestDto.getRemoteUsernameForUser());
        serverUser.setSshKeyForUser(sshKeyForUser); // Can be null
        serverUser.setAccessGrantedBy(adminUserId);

        savedServerUser = serverUserRepository.save(serverUser);
        logStatus = "SUCCESS";
        details = String.format("Access granted to user %s for server %s (ID: %s) by user %s. Remote username: %s. Key ID: %s",
                                requestDto.getPlatformUserId(), server.getName(), serverId, adminUserId,
                                requestDto.getRemoteUsernameForUser(), requestDto.getSshKeyIdForUser());
        activityLogService.createLog(adminUserId, "SERVER_ACCESS_GRANT", serverId, details, logStatus, null);
        return mapToServerUserResponse(savedServerUser);
        } catch (Exception e) {
            details = String.format("Failed to grant access to user %s for server ID %s by user %s. Remote username: %s. Key ID: %s. Error: %s",
                                    requestDto.getPlatformUserId(), serverId, adminUserId,
                                    requestDto.getRemoteUsernameForUser(), requestDto.getSshKeyIdForUser(), e.getMessage());
            // serverId might be null if the initial findById fails, but it's part of the input params
            activityLogService.createLog(adminUserId, "SERVER_ACCESS_GRANT_FAILED", serverId, details, logStatus, e.getMessage());
            throw e;
        }
    }

    @Transactional
    @CacheEvict(value = "userServerAccessDetails", key = "#targetPlatformUserId.toString() + ':' + #serverId.toString()") // Added
    public void revokeServerAccess(UUID adminUserId, UUID serverId, UUID targetPlatformUserId) {
        String details;
        String logStatus = "FAILED";
        Server server = null; // Initialize for logging in case of early failure

        try {
            server = serverRepository.findById(serverId)
                    .orElseThrow(() -> new ResourceNotFoundException("Server", "id", serverId));

            if (!server.getUserId().equals(adminUserId)) {
                throw new AccessDeniedException("User " + adminUserId + " does not own server " + serverId + ". Cannot revoke access.");
            }

            ServerUser serverUser = serverUserRepository.findByServerIdAndPlatformUserId(serverId, targetPlatformUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("ServerUser grant not found for user " + targetPlatformUserId + " on server " + serverId));
            
            serverUserRepository.delete(serverUser);
            logStatus = "SUCCESS";
            details = String.format("Access revoked for user %s from server %s (ID: %s) by user %s",
                                    targetPlatformUserId, server.getName(), serverId, adminUserId);
            activityLogService.createLog(adminUserId, "SERVER_ACCESS_REVOKE", serverId, details, logStatus, null);
        } catch (Exception e) {
            String serverNameForLog = server != null ? server.getName() : "N/A";
            details = String.format("Failed to revoke access for user %s from server %s (ID: %s) by user %s. Error: %s",
                                    targetPlatformUserId, serverNameForLog, serverId, adminUserId, e.getMessage());
            activityLogService.createLog(adminUserId, "SERVER_ACCESS_REVOKE_FAILED", serverId, details, logStatus, e.getMessage());
            throw e;
        }
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "userServerAccessDetails", key = "#platformUserId.toString() + ':' + #serverId.toString()") // Added
    public UserSpecificConnectionDetailsDto getValidatedConnectionDetails(UUID platformUserId, UUID serverId) {
        // Check if the platformUserId is the owner of the server first
        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new ResourceNotFoundException("Server", "id", serverId));

        if (server.getUserId().equals(platformUserId)) {
            // User is the owner, use server's default credentials
            UserSpecificConnectionDetailsDto details = new UserSpecificConnectionDetailsDto(
                    server.getHostname(),
                    server.getPort(),
                    server.getRemoteUsername(), // Server's default remote username
                    server.getAuthProvider()
            );
            if (server.getAuthProvider() == ServerAuthProvider.SSH_KEY) {
                if (server.getSshKey() == null) {
                    throw new IllegalStateException("Server " + serverId + " configured for SSH key auth but has no default key.");
                }
                // Decrypt server's default key. The owner (platformUserId) is allowed to use it.
                details.setDecryptedSshKey(sshKeyManagementService.getDecryptedSshKey(server.getSshKey().getId(), platformUserId));
            } else if (server.getAuthProvider() == ServerAuthProvider.PASSWORD) {
                // Decrypt server's default password. The owner (platformUserId) is allowed to use it.
                // This requires ServerManagementService to have a getDecryptedPassword method.
                details.setDecryptedPassword(serverManagementService.getDecryptedPassword(serverId, platformUserId));
            }
            return details;
        }

        // If not owner, look for a ServerUser grant
        ServerUser serverUser = serverUserRepository.findByServerIdAndPlatformUserId(serverId, platformUserId)
                .orElseThrow(() -> new AccessDeniedException("User " + platformUserId + " does not have access to server " + serverId));

        UserSpecificConnectionDetailsDto details = new UserSpecificConnectionDetailsDto(
                server.getHostname(),
                server.getPort(),
                serverUser.getRemoteUsernameForUser(), // User-specific remote username
                serverUser.getSshKeyForUser() != null ? ServerAuthProvider.SSH_KEY : server.getAuthProvider() // Infer auth provider
        );

        if (serverUser.getSshKeyForUser() != null) {
            details.setAuthProvider(ServerAuthProvider.SSH_KEY);
            // For the user-specific key, the 'platformUserId' requesting the connection details
            // is the one whose key it is (or is authorized to use).
            details.setDecryptedSshKey(sshKeyManagementService.getDecryptedSshKey(serverUser.getSshKeyForUser().getId(), platformUserId));
        } else {
            // No specific key for user, implies fallback to server's default method (if any)
            // This path is tricky: if server is SSH_KEY only and user has no specific key, it's an issue.
            // If server is PASSWORD, and user has no specific key, can they use server's password?
            // Current model ServerUser does not store user-specific password.
            // This initial version assumes ServerUser grant implies SSH key if SshKeyForUser is set.
            // Otherwise, it's an ill-defined state for this method if server's default is password.
            // For now, if no sshKeyForUser, authProvider might be inherited but credentials are not set in DTO.
             if (server.getAuthProvider() == ServerAuthProvider.PASSWORD) {
                 throw new AccessDeniedException("User " + platformUserId + " has a grant for server " + serverId + 
                                                 " but no specific SSH key, and server's default is PASSWORD auth. User-specific password auth not supported via ServerUser grant in this version.");
             } else if (server.getAuthProvider() == ServerAuthProvider.SSH_KEY && server.getSshKey() == null) {
                 throw new AccessDeniedException("User " + platformUserId + " has a grant for server " + serverId +
                                                 " but no specific SSH key, and server also has no default SSH key.");
             } else if (server.getAuthProvider() == ServerAuthProvider.SSH_KEY && server.getSshKey() != null) {
                 // User falls back to server's default SSH key.
                 // This implies the grant allows use of the server's default key.
                 // The platformUserId here should be the user trying to connect.
                 details.setDecryptedSshKey(sshKeyManagementService.getDecryptedSshKey(server.getSshKey().getId(), platformUserId));
                 details.setAuthProvider(ServerAuthProvider.SSH_KEY);

             }
        }
        return details;
    }

    private ServerUserResponse mapToServerUserResponse(ServerUser serverUser) {
        if (serverUser == null) return null;
        ServerUserResponse res = new ServerUserResponse(
                serverUser.getId(),
                serverUser.getServer().getId(),
                serverUser.getServer().getName(),
                serverUser.getPlatformUserId(),
                serverUser.getRemoteUsernameForUser(),
                serverUser.getSshKeyForUser() != null ? serverUser.getSshKeyForUser().getId() : null,
                serverUser.getSshKeyForUser() != null ? serverUser.getSshKeyForUser().getName() : null,
                serverUser.getAccessGrantedBy(),
                serverUser.getCreatedAt(),
                serverUser.getUpdatedAt()
        );
        return res;
    }
}
