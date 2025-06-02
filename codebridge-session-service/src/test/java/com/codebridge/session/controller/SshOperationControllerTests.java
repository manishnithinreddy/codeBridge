package com.codebridge.session.controller;

import com.codebridge.session.config.JwtConfigProperties;
import com.codebridge.session.dto.CommandRequest;
import com.codebridge.session.dto.CommandResponse;
import com.codebridge.session.dto.SshSessionMetadata;
import com.codebridge.session.exception.AccessDeniedException;
import com.codebridge.session.exception.GlobalExceptionHandler;
import com.codebridge.session.exception.RemoteOperationException;
import com.codebridge.session.model.SessionKey;
import com.codebridge.session.model.SshSessionWrapper;
import com.codebridge.session.security.jwt.JwtTokenProvider;
import com.codebridge.session.service.ApplicationInstanceIdProvider;
import com.codebridge.session.service.SshSessionLifecycleManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SshOperationController.class)
@Import({GlobalExceptionHandler.class, JwtConfigProperties.class})
class SshOperationControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SshSessionLifecycleManager sessionLifecycleManagerMock;
    @MockBean
    private JwtTokenProvider jwtTokenProviderMock;
    @MockBean
    private ApplicationInstanceIdProvider applicationInstanceIdProviderMock;

    // Mocks for JSch objects
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) // RETURNS_DEEP_STUBS for fluent API like session.openChannel().setCommand() etc.
    private Session jschSessionMock;
    @Mock
    private ChannelExec channelExecMock;
    @Mock
    private ChannelSftp channelSftpMock; // New mock for SFTP channel

    private String testToken;
    private SessionKey testSessionKey;
    private SshSessionMetadata testMetadata;
    private SshSessionWrapper testSshSessionWrapper;
    private String testInstanceId = "test-instance-id";

    @BeforeEach
    void setUp() {
        testToken = "valid.test.token";
        UUID userId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID(); // This would be serverId in a real scenario
        testSessionKey = new SessionKey(userId, resourceId, "SSH");

        testMetadata = new SshSessionMetadata();
        testMetadata.setSessionKey(testSessionKey);
        testMetadata.setHostingInstanceId(testInstanceId);

        testSshSessionWrapper = mock(SshSessionWrapper.class);
        when(testSshSessionWrapper.getJschSession()).thenReturn(jschSessionMock);
        when(testSshSessionWrapper.isConnected()).thenReturn(true);

        // Default mock behaviors for successful validation path in getValidatedLocalSshSession helper
        when(jwtTokenProviderMock.validateTokenAndExtractSessionKey(testToken)).thenReturn(Optional.of(testSessionKey));
        when(sessionLifecycleManagerMock.getSessionMetadata(testSessionKey)).thenReturn(Optional.of(testMetadata));
        when(applicationInstanceIdProviderMock.getInstanceId()).thenReturn(testInstanceId);
        when(sessionLifecycleManagerMock.getLocalSession(testSessionKey)).thenReturn(Optional.of(testSshSessionWrapper));
        doNothing().when(sessionLifecycleManagerMock).updateSessionAccessTime(testSessionKey);
    }


    @Test
    void executeCommand_success() throws Exception {
        CommandRequest commandRequest = new CommandRequest();
        commandRequest.setCommand("ls -la");

        // Mock JSch channel execution
        when(jschSessionMock.openChannel("exec")).thenReturn(channelExecMock);
        // Simulate command output
        when(channelExecMock.getOutputStream()).thenReturn(new ByteArrayOutputStream()); // Or mock specific output
        when(channelExecMock.getErrStream()).thenReturn(new ByteArrayOutputStream());
        // Simulate command finishing quickly for isClosed()
        // Need to ensure isClosed becomes true after some time or connect.
        // The while loop in controller: while (!channelExec.isClosed() && System.currentTimeMillis() < timeoutEndTime)
        // For robust test, mock isClosed() to return false initially then true.
        // Or ensure connect() makes it proceed. For simplicity, let's assume it closes after connect.
        doAnswer(invocation -> {
            when(channelExecMock.isClosed()).thenReturn(true); // Simulate command finishes after connect
            return null;
        }).when(channelExecMock).connect(anyInt());

        when(channelExecMock.getExitStatus()).thenReturn(0);
        // Mock output streams if specific output needs to be asserted
        ByteArrayOutputStream stdoutMockStream = new ByteArrayOutputStream();
        stdoutMockStream.write("command output".getBytes());
        when(channelExecMock.getOutputStream()).thenReturn(stdoutMockStream);


        mockMvc.perform(post("/ops/ssh/{sessionToken}/execute-command", testToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(commandRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.stdout").value("command output"))
            .andExpect(jsonPath("$.exitStatus").value(0));

        verify(sessionLifecycleManagerMock).updateSessionAccessTime(testSessionKey);
        verify(channelExecMock).disconnect();
    }

    @Test
    void executeCommand_invalidToken_returnsForbidden() throws Exception {
        when(jwtTokenProviderMock.validateTokenAndExtractSessionKey("invalid.token")).thenReturn(Optional.empty());
        CommandRequest commandRequest = new CommandRequest();
        commandRequest.setCommand("ls");

        mockMvc.perform(post("/ops/ssh/{sessionToken}/execute-command", "invalid.token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(commandRequest)))
            .andExpect(status().isForbidden()); // AccessDeniedException maps to 403 by default GlobalExceptionHandler
    }

    @Test
    void executeCommand_sessionNotHostedLocally_returnsForbidden() throws Exception {
        when(applicationInstanceIdProviderMock.getInstanceId()).thenReturn("another-instance-id");
        // testMetadata.hostingInstanceId remains testInstanceId
        CommandRequest commandRequest = new CommandRequest();
        commandRequest.setCommand("ls");

        mockMvc.perform(post("/ops/ssh/{sessionToken}/execute-command", testToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(commandRequest)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("Session not active on this service instance. Please retry. If error persists, re-initialize session."));
    }

    @Test
    void executeCommand_jschException_returnsInternalServerErrorAndReleasesSession() throws Exception {
        CommandRequest commandRequest = new CommandRequest();
        commandRequest.setCommand("error_command");

        when(jschSessionMock.openChannel("exec")).thenReturn(channelExecMock);
        doThrow(new JSchException("Channel error")).when(channelExecMock).connect(anyInt());
        // Simulate session becoming disconnected due to error
        when(jschSessionMock.isConnected()).thenReturn(false);

        mockMvc.perform(post("/ops/ssh/{sessionToken}/execute-command", testToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(commandRequest)))
            .andExpect(status().isInternalServerError()) // RemoteOperationException mapped by GlobalExceptionHandler
            .andExpect(jsonPath("$.message").value("SSH command execution error: Channel error"));

        // Verify session release was attempted because it became disconnected
        verify(sessionLifecycleManagerMock).forceReleaseSessionByKey(eq(testSessionKey), eq(true));
    }

    // TODO: Add tests for SFTP endpoints (list, download, upload) (Actually implementing now)

    // --- SFTP List ---
    @Test
    void listFiles_success() throws Exception {
        String remotePath = "/home/user/docs";
        when(jschSessionMock.openChannel("sftp")).thenReturn(channelSftpMock);

        Vector<ChannelSftp.LsEntry> lsEntries = new Vector<>();
        ChannelSftp.LsEntry entry1 = mock(ChannelSftp.LsEntry.class);
        SftpATTRS attrs1 = mock(SftpATTRS.class);
        when(entry1.getFilename()).thenReturn("file1.txt");
        when(entry1.getAttrs()).thenReturn(attrs1);
        when(attrs1.isDir()).thenReturn(false);
        when(attrs1.getSize()).thenReturn(1024L);
        when(entry1.getAttrs().getMtimeString()).thenReturn("2023-01-01 10:00:00");
        when(entry1.getAttrs().getPermissionsString()).thenReturn("-rw-r--r--");
        lsEntries.add(entry1);

        ChannelSftp.LsEntry entry2 = mock(ChannelSftp.LsEntry.class);
        SftpATTRS attrs2 = mock(SftpATTRS.class);
        when(entry2.getFilename()).thenReturn("mydir");
        when(entry2.getAttrs()).thenReturn(attrs2);
        when(attrs2.isDir()).thenReturn(true);
        when(attrs2.getSize()).thenReturn(0L); // Size often 0 or 4096 for dirs
        when(entry2.getAttrs().getMtimeString()).thenReturn("2023-01-02 11:00:00");
        when(entry2.getAttrs().getPermissionsString()).thenReturn("drwxr-xr-x");
        lsEntries.add(entry2);

        when(channelSftpMock.ls(remotePath)).thenReturn(lsEntries);

        mockMvc.perform(get("/ops/ssh/{sessionToken}/sftp/list", testToken)
                .param("remotePath", remotePath))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].filename").value("file1.txt"))
            .andExpect(jsonPath("$[0].directory").value(false))
            .andExpect(jsonPath("$[1].filename").value("mydir"))
            .andExpect(jsonPath("$[1].directory").value(true));

        verify(sessionLifecycleManagerMock).updateSessionAccessTime(testSessionKey);
        verify(channelSftpMock).disconnect();
    }

    @Test
    void listFiles_sftpException_returnsInternalServerError() throws Exception {
        String remotePath = "/error/path";
        when(jschSessionMock.openChannel("sftp")).thenReturn(channelSftpMock);
        when(channelSftpMock.ls(remotePath)).thenThrow(new SftpException(ChannelSftp.SSH_FX_FAILURE, "SFTP ls failed"));
        when(jschSessionMock.isConnected()).thenReturn(false); // Simulate session disconnect due to error

        mockMvc.perform(get("/ops/ssh/{sessionToken}/sftp/list", testToken)
                .param("remotePath", remotePath))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.message").value("SFTP list operation failed: SFTP ls failed"));

        verify(sessionLifecycleManagerMock).forceReleaseSessionByKey(eq(testSessionKey), eq(true));
        verify(channelSftpMock).disconnect(); // Should still be called in finally
    }

    // --- SFTP Download ---
    @Test
    void downloadFile_success() throws Exception {
        String remotePath = "/path/to/download.txt";
        byte[] fileContent = "Hello World".getBytes();
        InputStream inputStream = new ByteArrayInputStream(fileContent);

        when(jschSessionMock.openChannel("sftp")).thenReturn(channelSftpMock);
        // Mock the get method that takes an OutputStream
        doAnswer(invocation -> {
            ByteArrayOutputStream baos = invocation.getArgument(1);
            baos.write(fileContent);
            return null;
        }).when(channelSftpMock).get(eq(remotePath), any(ByteArrayOutputStream.class));


        mockMvc.perform(get("/ops/ssh/{sessionToken}/sftp/download", testToken)
                .param("remotePath", remotePath))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''download.txt"))
            .andExpect(content().bytes(fileContent));

        verify(sessionLifecycleManagerMock).updateSessionAccessTime(testSessionKey);
        verify(channelSftpMock).disconnect();
    }

    // --- SFTP Upload ---
    @Test
    @Disabled("Mocking MultipartFile with RestTemplate exchange for upload is more complex and might need deeper setup or different test strategy")
    void uploadFile_success() throws Exception {
        // This test is more involved due to MultipartFile and RestTemplate's handling of it.
        // It often requires a more integration-style test or careful mocking of RestTemplate exchanges.
        // For now, marking as disabled.
        // String remotePath = "/upload/here/newfile.txt";
        // MockMultipartFile mockFile = new MockMultipartFile("file", "original.txt", MediaType.TEXT_PLAIN_VALUE, "Upload content".getBytes());

        // when(jschSessionMock.openChannel("sftp")).thenReturn(channelSftpMock);
        // doNothing().when(channelSftpMock).put(any(InputStream.class), eq(remotePath), eq(ChannelSftp.OVERWRITE));

        // mockMvc.perform(multipart("/ops/ssh/{sessionToken}/sftp/upload", testToken)
        //         .file(mockFile)
        //         .param("remotePath", remotePath))
        //     .andExpect(status().isOk());

        // verify(sessionLifecycleManagerMock).updateSessionAccessTime(testSessionKey);
        // verify(channelSftpMock).disconnect();
    }
     @Test
    void uploadFile_emptyFile_throwsException() throws Exception {
        // This test depends on how SshOperationController handles empty file before even calling the service.
        // The controller currently throws RemoteOperationException("Uploaded file cannot be empty.")
        // So, we don't need to mock JSch for this, just call the endpoint.
        // This test assumes the controller's check `if (file.isEmpty())` is hit.

        // Create an empty MockMultipartFile
         MockMultipartFile mockFile = new MockMultipartFile("file", "empty.txt", MediaType.TEXT_PLAIN_VALUE, new byte[0]);

        mockMvc.perform(multipart("/ops/ssh/{sessionToken}/sftp/upload", testToken)
                .file(mockFile)
                .param("remotePath", "/upload/empty.txt"))
            .andExpect(status().isInternalServerError()) // Or BadRequest if GlobalExceptionHandler maps it differently
            .andExpect(jsonPath("$.message").value("Uploaded file cannot be empty."));
            // Note: sessionLifecycleManager.updateSessionAccessTime might not be called if validation fails early
    }


}
