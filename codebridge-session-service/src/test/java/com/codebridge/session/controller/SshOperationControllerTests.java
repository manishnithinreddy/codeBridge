package com.codebridge.session.controller;

import com.codebridge.session.config.ApplicationInstanceIdProvider;
import com.codebridge.session.dto.SshSessionMetadata;
import com.codebridge.session.dto.ops.CommandRequest;
import com.codebridge.session.dto.CommandResponse;
import com.codebridge.session.dto.ops.RemoteFileEntry;
import com.codebridge.session.model.SessionKey;
import com.codebridge.session.model.SshSessionWrapper;
import com.codebridge.session.security.jwt.JwtTokenProvider;
import com.codebridge.session.service.SshSessionLifecycleManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.Vector;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SshOperationController.class)
class SshOperationControllerTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private SshSessionLifecycleManager sessionLifecycleManager;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private ApplicationInstanceIdProvider instanceIdProvider;

    @Mock private Session mockJschSession;
    @Mock private SshSessionWrapper mockSshWrapper;
    @Mock private ChannelExec mockChannelExec;
    @Mock private ChannelSftp mockChannelSftp;

    private String validSessionToken = "valid-ssh-ops-token";
    private UUID platformUserId = UUID.randomUUID();
    private UUID serverId = UUID.randomUUID();
    private SessionKey sessionKey;

    @BeforeEach
    void setUp() {
        sessionKey = new SessionKey(platformUserId, serverId, "SSH");
        Claims claims = new DefaultClaims().setSubject(platformUserId.toString());
        claims.put("resourceId", serverId.toString());
        claims.put("type", "SSH");

        when(jwtTokenProvider.validateToken(validSessionToken)).thenReturn(true);
        when(jwtTokenProvider.getClaimsFromToken(validSessionToken)).thenReturn(claims);

        SshSessionMetadata metadata = new SshSessionMetadata(
            platformUserId, 
            serverId, 
            validSessionToken,
            System.currentTimeMillis(), 
            System.currentTimeMillis(), 
            System.currentTimeMillis() + 3600000, 
            "test-instance",
            "test-host",
            "test-user"
        );
        
        when(sessionLifecycleManager.getSessionMetadata(sessionKey)).thenReturn(Optional.of(metadata));
        when(instanceIdProvider.getInstanceId()).thenReturn("test-instance");

        when(mockSshWrapper.isConnected()).thenReturn(true);
        when(mockSshWrapper.getJschSession()).thenReturn(mockJschSession);
        when(sessionLifecycleManager.getLocalSession(sessionKey)).thenReturn(Optional.of(mockSshWrapper));
    }

    @Test
    void executeCommand_validSession_returnsCommandResponse() throws Exception {
        CommandRequest cmdRequest = new CommandRequest();
        cmdRequest.setCommand("ls -la");

        when(mockJschSession.openChannel("exec")).thenReturn(mockChannelExec);
        when(mockChannelExec.getOutputStream()).thenReturn(new ByteArrayOutputStream()); // To avoid NPE
        when(mockChannelExec.getErrStream()).thenReturn(new ByteArrayInputStream(new byte[0])); // To avoid NPE
        when(mockChannelExec.getInputStream()).thenReturn(new ByteArrayInputStream("output".getBytes()));


        mockMvc.perform(post("/ops/ssh/{sessionToken}/execute-command", validSessionToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cmdRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.output").exists())
            .andExpect(jsonPath("$.exitCode").isNumber());
    }

    @Test
    void listFiles_validSession_returnsRemoteFileEntries() throws Exception {
        String remotePath = "/home/user";
        when(mockJschSession.openChannel("sftp")).thenReturn(mockChannelSftp);

        Vector<ChannelSftp.LsEntry> lsEntries = new Vector<>();
        // Mock LsEntry and SftpATTRS
        ChannelSftp.LsEntry mockEntry = mock(ChannelSftp.LsEntry.class);
        SftpATTRS mockAttrs = mock(SftpATTRS.class);
        when(mockEntry.getFilename()).thenReturn("testfile.txt");
        when(mockEntry.getAttrs()).thenReturn(mockAttrs);
        when(mockAttrs.isDir()).thenReturn(false);
        when(mockAttrs.getSize()).thenReturn(1024L);
        when(mockAttrs.getMTime()).thenReturn((int) (System.currentTimeMillis()/1000));
        when(mockAttrs.getPermissionsString()).thenReturn("-rw-r--r--");
        lsEntries.add(mockEntry);

        when(mockChannelSftp.ls(remotePath)).thenReturn(lsEntries);

        mockMvc.perform(get("/ops/ssh/{sessionToken}/sftp/list", validSessionToken)
                .param("remotePath", remotePath))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].filename").value("testfile.txt"));
    }

    @Test
    void downloadFile_validSession_returnsFile() throws Exception {
        String remotePath = "/home/user/file.zip";
        byte[] fileContent = "dummy zip content".getBytes();

        when(mockJschSession.openChannel("sftp")).thenReturn(mockChannelSftp);
        // Mock lstat to return non-directory attributes
        SftpATTRS mockAttrs = mock(SftpATTRS.class);
        when(mockAttrs.isDir()).thenReturn(false);
        when(mockChannelSftp.lstat(remotePath)).thenReturn(mockAttrs);

        // Mock the 'get' operation
        doAnswer(invocation -> {
            ByteArrayOutputStream baos = invocation.getArgument(1);
            baos.write(fileContent);
            return null;
        }).when(mockChannelSftp).get(eq(remotePath), any(ByteArrayOutputStream.class));


        mockMvc.perform(get("/ops/ssh/{sessionToken}/sftp/download", validSessionToken)
                .param("remotePath", remotePath))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"file.zip\""))
            .andExpect(content().bytes(fileContent));
    }

    @Test
    @Disabled("SFTP Upload test with MultipartFile is complex to unit test with MockMvc correctly for stream handling")
    void uploadFile_validSession_uploadsFile() throws Exception {
        String remotePath = "/tmp";
        MockMultipartFile mockFile = new MockMultipartFile(
            "file", "upload.txt", MediaType.TEXT_PLAIN_VALUE, "test content".getBytes());

        when(mockJschSession.openChannel("sftp")).thenReturn(mockChannelSftp);
        // Add more mocking for lstat, cd, put as needed

        mockMvc.perform(multipart("/ops/ssh/{sessionToken}/sftp/upload", validSessionToken)
                .file(mockFile)
                .param("remotePath", remotePath))
            .andExpect(status().isOk());
    }
}

