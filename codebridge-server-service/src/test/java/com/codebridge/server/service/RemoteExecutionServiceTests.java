package com.codebridge.server.service;

import com.codebridge.server.dto.remote.CommandRequest;
import com.codebridge.server.dto.remote.CommandResponse;
import com.codebridge.server.exception.AccessDeniedException;
import com.codebridge.server.exception.RemoteCommandException;
import com.codebridge.server.exception.ResourceNotFoundException;
import com.codebridge.server.model.Server;
import com.codebridge.server.model.SshKey;
import com.codebridge.server.model.enums.ServerAuthProvider;
import com.codebridge.server.service.ServerAccessControlService.UserSpecificConnectionDetails;

import com.jcraft.jsch.JSchException; // Import for mocking
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RemoteExecutionServiceTests {

    @Mock
    private ServerAccessControlService serverAccessControlService;
    
    @Mock
    private ServerActivityLogService serverActivityLogService; // Mock for logging calls

    @InjectMocks
    private RemoteExecutionService remoteExecutionService;

    private UUID testUserId;
    private UUID testServerId;
    private CommandRequest commandRequest;
    private UserSpecificConnectionDetails connectionDetails;
    private Server server;
    private SshKey decryptedSshKey;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testServerId = UUID.randomUUID();

        commandRequest = new CommandRequest();
        commandRequest.setCommand("ls -la");
        commandRequest.setTimeout(30);

        server = new Server();
        server.setId(testServerId);
        server.setHostname("testhost");
        server.setPort(22);
        server.setAuthProvider(ServerAuthProvider.SSH_KEY); // Ensure this is set for key-based auth path

        decryptedSshKey = new SshKey();
        decryptedSshKey.setId(UUID.randomUUID());
        decryptedSshKey.setPrivateKey("decrypted-private-key-content");
        // Public key might be null if not stored/retrieved, ensure JSch part handles this
        decryptedSshKey.setPublicKey("ssh-rsa AAA...");


        connectionDetails = new UserSpecificConnectionDetails(server, "testremoteuser", decryptedSshKey);
        
        // Mock successful log creation
        doNothing().when(serverActivityLogService).createLog(any(), anyString(), any(), anyString(), anyString(), anyString());
    }

    @Test
    void executeCommand_successPath_conceptual() {
        when(serverAccessControlService.checkUserAccessAndGetConnectionDetails(testUserId, testServerId))
                .thenReturn(connectionDetails);

        // Conceptual: We cannot easily mock the JSch session and channel execution here
        // without significant refactoring or PowerMock.
        // This test will likely attempt a real JSch connection if not stopped.
        // For a true unit test, we'd mock JSch session/channel or a wrapper.
        
        // We expect a RemoteCommandException because a real SSH connection will fail in test environment.
        // This verifies that the initial setup (fetching details) works before JSch part.
        RemoteCommandException RCEexception = assertThrows(RemoteCommandException.class, () -> {
            remoteExecutionService.executeCommand(testServerId, testUserId, commandRequest);
        });
        // The error message will likely be from JSch connection attempt (e.g., "Auth fail", "timeout", "Host not found")
        assertNotNull(RCEexception.getMessage());
        
        // Verify activity log for connection attempt (even if it fails at JSch level)
        // The exact logging depends on where it's placed if JSch fails.
        // Currently, logging is after successful command execution or specific JSchException.
        // If connection fails very early (e.g. getSession), it might throw JSchException before command logging.
        
        // To make this test more specific for SUCCESS, we would need to mock JSch execution.
        // For now, this test path shows that the service attempts connection after getting details.
        System.out.println("Conceptual SUCCESS test: JSch part threw (as expected in unit test env): " + RCEexception.getMessage());
         verify(serverActivityLogService, atLeastOnce()).createLog(
            eq(testUserId), 
            eq("EXECUTE_COMMAND"), 
            eq(testServerId), 
            anyString(), // Details will vary based on actual outcome
            anyString(), // Status will vary
            any()        // Error message will vary
        );
    }
    
    @Test
    void executeCommand_whenAccessDenied_throwsAccessDeniedException() {
        when(serverAccessControlService.checkUserAccessAndGetConnectionDetails(testUserId, testServerId))
                .thenThrow(new AccessDeniedException("User does not have access"));

        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () -> {
            remoteExecutionService.executeCommand(testServerId, testUserId, commandRequest);
        });

        assertEquals("User does not have access", exception.getMessage());
         verify(serverActivityLogService).createLog(
            eq(testUserId), 
            eq("EXECUTE_COMMAND"), 
            eq(testServerId), 
            contains("Access denied for command: ls -la"), 
            eq("FAILURE"), 
            eq("User does not have access")
        );
    }

    @Test
    void executeCommand_whenServerNotFound_throwsResourceNotFoundException() {
        when(serverAccessControlService.checkUserAccessAndGetConnectionDetails(testUserId, testServerId))
                .thenThrow(new ResourceNotFoundException("Server", "id", testServerId));

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            remoteExecutionService.executeCommand(testServerId, testUserId, commandRequest);
        });

        assertTrue(exception.getMessage().contains("Server not found with id : '" + testServerId));
         verify(serverActivityLogService).createLog(
            eq(testUserId), 
            eq("EXECUTE_COMMAND"), 
            eq(testServerId), 
            contains("Server not found for command: ls -la"), 
            eq("FAILURE"), 
            contains("Server not found with id : '" + testServerId)
        );
    }
    
    @Test
    void executeCommand_serverNotConfiguredForSshKey_throwsRemoteCommandException() {
        server.setAuthProvider(ServerAuthProvider.PASSWORD); // Set to non-SSH_KEY auth
        connectionDetails = new UserSpecificConnectionDetails(server, "testremoteuser", null); // SSH key would be null

        when(serverAccessControlService.checkUserAccessAndGetConnectionDetails(testUserId, testServerId))
                .thenReturn(connectionDetails);
        
        RemoteCommandException exception = assertThrows(RemoteCommandException.class, () -> {
            remoteExecutionService.executeCommand(testServerId, testUserId, commandRequest);
        });
        assertTrue(exception.getMessage().contains("Server is not configured for SSH Key authentication"));
         verify(serverActivityLogService).createLog(
            eq(testUserId), 
            eq("EXECUTE_COMMAND"), 
            eq(testServerId), 
            contains("Unsupported auth provider: PASSWORD"), 
            eq("FAILURE"), 
            eq("Command execution failed: Server is not configured for SSH Key authentication.")
        );
    }

    @Test
    void executeCommand_sshKeyIdMissing_throwsRemoteCommandException() {
        // This scenario is now primarily handled by ServerAccessControlService, 
        // but if it returned a Server object that somehow missed this, RemoteExecutionService would catch it.
        // Let's assume ServerAccessControlService returns valid Server object but SshKey is null for an SSH_KEY server user grant.
        server.setAuthProvider(ServerAuthProvider.SSH_KEY); // Server expects SSH Key
        // Simulate ServerUser record has no SshKeyForUser, so decryptedSshKey is null
        connectionDetails = new UserSpecificConnectionDetails(server, "testremoteuser", null); 
                                                            
        when(serverAccessControlService.checkUserAccessAndGetConnectionDetails(testUserId, testServerId))
                .thenReturn(connectionDetails);

        RemoteCommandException exception = assertThrows(RemoteCommandException.class, () -> {
            remoteExecutionService.executeCommand(testServerId, testUserId, commandRequest);
        });
        assertTrue(exception.getMessage().contains("Private key is missing or empty for SSH key"));
         verify(serverActivityLogService).createLog(
            eq(testUserId), 
            eq("EXECUTE_COMMAND"), 
            eq(testServerId), 
            contains("Decrypted private key is empty."), 
            eq("FAILURE"), 
            contains("Private key is missing or empty for SSH key")
        );
    }
    
    @Test
    void executeCommand_decryptedPrivateKeyMissing_throwsRemoteCommandException() {
        decryptedSshKey.setPrivateKey(null); // Simulate missing private key after decryption
        connectionDetails = new UserSpecificConnectionDetails(server, "testremoteuser", decryptedSshKey);

        when(serverAccessControlService.checkUserAccessAndGetConnectionDetails(testUserId, testServerId))
                .thenReturn(connectionDetails);
        
        RemoteCommandException exception = assertThrows(RemoteCommandException.class, () -> {
            remoteExecutionService.executeCommand(testServerId, testUserId, commandRequest);
        });
        assertTrue(exception.getMessage().contains("Private key is missing or empty for SSH key"));
         verify(serverActivityLogService).createLog(
            eq(testUserId), 
            eq("EXECUTE_COMMAND"), 
            eq(testServerId), 
            contains("Decrypted private key is empty."), 
            eq("FAILURE"), 
            contains("Private key is missing or empty for SSH key")
        );
    }
    
    // Conceptual test for JSchException during connection
    @Test
    void executeCommand_jschConnectionError_throwsRemoteCommandException() {
        when(serverAccessControlService.checkUserAccessAndGetConnectionDetails(testUserId, testServerId))
                .thenReturn(connectionDetails);
        
        // To properly test this, we'd need to mock JSch's getSession().connect() to throw JSchException.
        // Since we are not deeply mocking JSch, we acknowledge this test is conceptual.
        // The current successPath test will likely hit this if example.com is not reachable with the key.
        assertThrows(RemoteCommandException.class, () -> {
             remoteExecutionService.executeCommand(testServerId, testUserId, commandRequest);
        }, "Expected RemoteCommandException due to JSch connection issues in test environment.");
        
        // Verification of logging in the actual JSchException catch block
        // This will only be hit if the JSchException is thrown as expected
         verify(serverActivityLogService, atLeastOnce()).createLog( // atLeastOnce because other logs might occur first
            eq(testUserId),
            eq("EXECUTE_COMMAND"),
            eq(testServerId),
            matches("JSchException: .*|SSH connection or command execution error: .*"), // Details will contain JSch specific error
            eq("FAILURE"),
            matches("JSchException: .*|SSH connection or command execution error: .*")
        );
    }
}
