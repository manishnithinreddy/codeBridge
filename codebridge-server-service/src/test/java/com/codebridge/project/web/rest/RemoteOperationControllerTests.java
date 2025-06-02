package com.codebridge.project.web.rest;

import com.codebridge.project.security.SecurityUtils;
import com.codebridge.project.service.RemoteExecutionService;
import com.codebridge.project.service.dto.CommandRequest;
import com.codebridge.project.service.dto.CommandResponse;
import com.codebridge.project.service.exception.AccessDeniedException;
import com.codebridge.project.service.exception.RemoteCommandException;
import com.codebridge.project.service.exception.ResourceNotFoundException;
import com.codebridge.project.web.rest.errors.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RemoteOperationController.class)
@Import(GlobalExceptionHandler.class)
class RemoteOperationControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RemoteExecutionService remoteExecutionService;

    @Autowired
    private ObjectMapper objectMapper;

    private String mockUserId;
    private String serverId;

    @BeforeEach
    void setUp() {
        mockUserId = UUID.randomUUID().toString();
        serverId = UUID.randomUUID().toString();
    }

    private CommandRequest validCommandRequest() {
        CommandRequest request = new CommandRequest();
        request.setCommand("ls -la");
        return request;
    }

    @Test
    void executeCommand_shouldReturnCommandResponse() throws Exception {
        CommandRequest request = validCommandRequest();
        CommandResponse response = new CommandResponse();
        response.setOutput("total 0\ndrwxr-xr-x 1 user user 0 Jan 1 00:00 .");
        response.setError("");
        response.setExitCode(0);

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.of(mockUserId));
            when(remoteExecutionService.executeCommand(eq(mockUserId), eq(serverId), any(CommandRequest.class)))
                .thenReturn(response);

            mockMvc.perform(post("/api/servers/{serverId}/remote/execute-command", serverId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.output").value(response.getOutput()))
                .andExpect(jsonPath("$.exitCode").value(0));
        }
    }

    @Test
    void executeCommand_whenCommandIsBlank_shouldReturnBadRequest() throws Exception {
        CommandRequest request = new CommandRequest();
        request.setCommand(""); // Invalid

        mockMvc.perform(post("/api/servers/{serverId}/remote/execute-command", serverId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("command"))
            .andExpect(jsonPath("$.errors[0].message").value("Command cannot be blank."));
    }

    @Test
    void executeCommand_whenCommandIsNull_shouldReturnBadRequest() throws Exception {
        CommandRequest request = new CommandRequest();
        request.setCommand(null); // Invalid

        mockMvc.perform(post("/api/servers/{serverId}/remote/execute-command", serverId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("command"))
            .andExpect(jsonPath("$.errors[0].message").value("Command cannot be blank."));
    }


    @Test
    void executeCommand_whenServerNotFound_shouldReturnNotFound() throws Exception {
        CommandRequest request = validCommandRequest();

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.of(mockUserId));
            when(remoteExecutionService.executeCommand(eq(mockUserId), eq(serverId), any(CommandRequest.class)))
                .thenThrow(new ResourceNotFoundException("Server not found"));

            mockMvc.perform(post("/api/servers/{serverId}/remote/execute-command", serverId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Server not found"));
        }
    }

    @Test
    void executeCommand_whenAccessDenied_shouldReturnForbidden() throws Exception {
        CommandRequest request = validCommandRequest();

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.of(mockUserId));
            when(remoteExecutionService.executeCommand(eq(mockUserId), eq(serverId), any(CommandRequest.class)))
                .thenThrow(new AccessDeniedException("Access denied to this server"));

            mockMvc.perform(post("/api/servers/{serverId}/remote/execute-command", serverId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied to this server"));
        }
    }

    @Test
    void executeCommand_whenRemoteCommandException_shouldReturnInternalServerError() throws Exception {
        CommandRequest request = validCommandRequest();

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.of(mockUserId));
            when(remoteExecutionService.executeCommand(eq(mockUserId), eq(serverId), any(CommandRequest.class)))
                .thenThrow(new RemoteCommandException("SSH connection failed"));

            mockMvc.perform(post("/api/servers/{serverId}/remote/execute-command", serverId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("SSH connection failed"));
        }
    }
}
