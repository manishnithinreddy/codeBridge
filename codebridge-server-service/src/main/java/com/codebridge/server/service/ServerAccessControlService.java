package com.codebridge.server.service;

import com.codebridge.server.dto.ServerUserRequest;
import com.codebridge.server.dto.ServerUserResponse;
import com.codebridge.server.exception.AccessDeniedException;
import com.codebridge.server.exception.ResourceNotFoundException;
import com.codebridge.server.model.Server;
import com.codebridge.server.model.ServerUser;
import com.codebridge.server.model.SshKey;
import com.codebridge.server.repository.ServerRepository;
import com.codebridge.server.repository.ServerUserRepository;
import com.codebridge.server.repository.SshKeyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ServerAccessControlService {

    private final ServerUserRepository serverUserRepository;
    private final ServerRepository serverRepository;
    private final SshKeyRepository sshKeyRepository; 
    private final SshKeyManagementService sshKeyManagementService; 

    // Define the record for connection details
    public record UserSpecificConnectionDetails(
        Server server, 
        String remoteUsername, 
        SshKey decryptedSshKey // This SshKey instance will contain the decrypted private key
    ) {}

    public ServerAccessControlService(ServerUserRepository serverUserRepository,
                                      ServerRepository serverRepository,
                                      SshKeyRepository sshKeyRepository,
                                      SshKeyManagementService sshKeyManagementService) {
        this.serverUserRepository = serverUserRepository;
        this.serverRepository = serverRepository;
        this.sshKeyRepository = sshKeyRepository;
        this.sshKeyManagementService = sshKeyManagementService;
    }

    @Transactional
    public ServerUserResponse grantServerAccess(UUID adminUserId, ServerUserRequest requestDto) {
        Server server = serverRepository.findById(requestDto.getServerId())
                .orElseThrow(() -> new ResourceNotFoundException("Server", "id", requestDto.getServerId()));

        // Simplified ownership check: admin must be the one who registered the server.
        if (!server.getUserId().equals(adminUserId)) {
            throw new AccessDeniedException("Admin user " + adminUserId + " does not have rights to grant access to server " + server.getId());
        }

        SshKey sshKeyToUse = null;
        if (requestDto.getSshKeyIdForUser() != null) {
            // We assume the adminUserId implies the right to assign this key.
            // A more complex system might check if adminUserId owns the key or if it's a shared team key.
            sshKeyToUse = sshKeyRepository.findById(requestDto.getSshKeyIdForUser()) 
                    .orElseThrow(() -> new ResourceNotFoundException("SshKey", "id", requestDto.getSshKeyIdForUser()));
        } else {
            // If the server itself is SSH_KEY authenticated, a key is required for the user.
            if (server.getAuthProvider() == com.codebridge.server.model.enums.ServerAuthProvider.SSH_KEY) {
                 throw new IllegalArgumentException("SSH Key ID must be provided when granting access to an SSH-key authenticated server for a user.");
            }
            // If server is password-based, sshKeyToUse can remain null.
        }

        ServerUser serverUser = serverUserRepository
                .findByServerIdAndPlatformUserId(requestDto.getServerId(), requestDto.getPlatformUserId())
                .orElse(new ServerUser());

        serverUser.setServer(server);
        serverUser.setPlatformUserId(requestDto.getPlatformUserId());
        serverUser.setRemoteUsernameForUser(requestDto.getRemoteUsernameForUser());
        serverUser.setSshKeyForUser(sshKeyToUse);
        serverUser.setAccessGrantedBy(adminUserId);

        ServerUser savedServerUser = serverUserRepository.save(serverUser);
        return mapToServerUserResponse(savedServerUser);
    }

    @Transactional
    public void revokeServerAccess(UUID adminUserId, UUID serverId, UUID targetPlatformUserId) {
        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new ResourceNotFoundException("Server", "id", serverId));
        if (!server.getUserId().equals(adminUserId)) { 
            throw new AccessDeniedException("Admin user " + adminUserId + " does not have rights to revoke access to server " + serverId);
        }

        ServerUser serverUser = serverUserRepository.findByServerIdAndPlatformUserId(serverId, targetPlatformUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ServerUser access grant not found for server " + serverId + " and user " + targetPlatformUserId));
        
        serverUserRepository.delete(serverUser);
    }


    @Transactional(readOnly = true)
    public UserSpecificConnectionDetails checkUserAccessAndGetConnectionDetails(UUID platformUserId, UUID serverId) {
        ServerUser serverUser = serverUserRepository.findByServerIdAndPlatformUserId(serverId, platformUserId)
                .orElseThrow(() -> new AccessDeniedException("User " + platformUserId + " does not have access to server " + serverId));

        Server server = serverUser.getServer(); 
        if (server == null) { 
             throw new IllegalStateException("ServerUser record " + serverUser.getId() + " has no associated Server. Data integrity issue.");
        }
        
        SshKey decryptedSshKey = null;
        if (serverUser.getSshKeyForUser() != null) {
            // The SshKeyManagementService.getDecryptedSshKey will be modified to only take keyId.
            // The authorization to use this key is established by the existence of this ServerUser record,
            // which was granted by an admin.
            decryptedSshKey = sshKeyManagementService.getDecryptedSshKey(serverUser.getSshKeyForUser().getId());
        } else if (server.getAuthProvider() == com.codebridge.server.model.enums.ServerAuthProvider.SSH_KEY) {
            // This case means the server requires an SSH key, but the user-specific grant doesn't specify one.
            // This is a configuration error for this user's access grant.
            throw new AccessDeniedException("User " + platformUserId + " has no SSH key assigned for this SSH-key authenticated server " + serverId + ". Access grant is misconfigured.");
        }
        // If server.authProvider is PASSWORD, decryptedSshKey will correctly be null.
        // The calling services (RemoteExecutionService, FileTransferService) would then need to handle
        // password-based authentication if that's a supported path for them (currently, they focus on SSH keys).

        return new UserSpecificConnectionDetails(server, serverUser.getRemoteUsernameForUser(), decryptedSshKey);
    }

    private ServerUserResponse mapToServerUserResponse(ServerUser serverUser) {
        ServerUserResponse response = new ServerUserResponse();
        response.setId(serverUser.getId());
        response.setServerId(serverUser.getServer().getId());
        response.setServerName(serverUser.getServer().getName()); 
        response.setPlatformUserId(serverUser.getPlatformUserId());
        response.setRemoteUsernameForUser(serverUser.getRemoteUsernameForUser());
        if (serverUser.getSshKeyForUser() != null) {
            response.setSshKeyIdForUser(serverUser.getSshKeyForUser().getId());
            response.setSshKeyNameForUser(serverUser.getSshKeyForUser().getName()); 
        }
        response.setAccessGrantedBy(serverUser.getAccessGrantedBy());
        response.setCreatedAt(serverUser.getCreatedAt());
        response.setUpdatedAt(serverUser.getUpdatedAt());
        return response;
    }
}
