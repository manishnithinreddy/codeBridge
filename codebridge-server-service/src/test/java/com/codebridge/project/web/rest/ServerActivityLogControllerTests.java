package com.codebridge.project.web.rest;

import com.codebridge.project.security.SecurityUtils;
import com.codebridge.project.service.ServerActivityLogService;
import com.codebridge.project.service.dto.ServerActivityLogResponse;
import com.codebridge.project.web.rest.errors.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ServerActivityLogController.class)
@Import(GlobalExceptionHandler.class)
class ServerActivityLogControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ServerActivityLogService serverActivityLogService;

    @Autowired
    private ObjectMapper objectMapper;

    private String mockAdminUserId;
    private String targetUserId;
    private String serverId;

    @BeforeEach
    void setUp() {
        mockAdminUserId = UUID.randomUUID().toString(); // User performing the request
        targetUserId = UUID.randomUUID().toString();    // User whose logs are being fetched
        serverId = UUID.randomUUID().toString();
    }

    private ServerActivityLogResponse sampleLogResponse() {
        ServerActivityLogResponse log = new ServerActivityLogResponse();
        log.setId("log-id-1");
        log.setServerId(serverId);
        log.setServerName("Test Server");
        log.setUserId(targetUserId);
        log.setAction("SERVER_CREATED");
        log.setTimestamp(Instant.now());
        log.setSuccess(true);
        return log;
    }

    @Test
    void getLogsForServer_shouldReturnPageOfLogs() throws Exception {
        Page<ServerActivityLogResponse> logPage = new PageImpl<>(Collections.singletonList(sampleLogResponse()), PageRequest.of(0, 10), 1);

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.of(mockAdminUserId));
            // Assuming any admin can view logs for any server. Access control might be more granular.
            when(serverActivityLogService.getLogsForServer(eq(serverId), any(Pageable.class))).thenReturn(logPage);

            mockMvc.perform(get("/api/activity-logs/server/{serverId}", serverId)
                    .param("page", "0")
                    .param("size", "10")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("log-id-1"))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
        }
    }

    @Test
    void getLogsForServer_whenNoLogs_shouldReturnEmptyPage() throws Exception {
        Page<ServerActivityLogResponse> emptyPage = Page.empty(PageRequest.of(0,10));

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.of(mockAdminUserId));
            when(serverActivityLogService.getLogsForServer(eq(serverId), any(Pageable.class))).thenReturn(emptyPage);

            mockMvc.perform(get("/api/activity-logs/server/{serverId}", serverId)
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
        }
    }

    // Consider adding test for ResourceNotFoundException if serverActivityLogService.getLogsForServer could throw it
    // e.g. if the server itself must exist to fetch logs. Often, logs might still exist or return empty.

    @Test
    void getLogsForUser_shouldReturnPageOfLogs() throws Exception {
        Page<ServerActivityLogResponse> logPage = new PageImpl<>(Collections.singletonList(sampleLogResponse()), PageRequest.of(0, 5), 1);

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            // Assume an admin is making this request, or a user is fetching their own logs.
            // The controller's SecurityUtils.getCurrentUserId() will be the one performing the action.
            // The {platformUserId} is the target user for the logs.
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.of(mockAdminUserId));

            // This mock implies an admin (mockAdminUserId) is fetching logs for targetUserId
            when(serverActivityLogService.getLogsForUser(eq(targetUserId), any(Pageable.class))).thenReturn(logPage);

            mockMvc.perform(get("/api/activity-logs/user/{platformUserId}", targetUserId)
                    .param("size", "5")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].userId").value(targetUserId))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.numberOfElements").value(1));
        }
    }

    @Test
    void getLogsForUser_whenFetchingOwnLogs_shouldReturnPageOfLogs() throws Exception {
        Page<ServerActivityLogResponse> logPage = new PageImpl<>(Collections.singletonList(sampleLogResponse()), PageRequest.of(0, 5), 1);
        // In this scenario, the current user IS the target user.
        String currentUserId = targetUserId;

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.of(currentUserId));

            when(serverActivityLogService.getLogsForUser(eq(currentUserId), any(Pageable.class))).thenReturn(logPage);

            mockMvc.perform(get("/api/activity-logs/user/{platformUserId}", currentUserId)
                    .param("size", "5")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].userId").value(currentUserId));
        }
    }


    @Test
    void getLogsForUser_whenNoLogs_shouldReturnEmptyPage() throws Exception {
        Page<ServerActivityLogResponse> emptyPage = Page.empty(PageRequest.of(0,10));

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.of(mockAdminUserId));
            when(serverActivityLogService.getLogsForUser(eq(targetUserId), any(Pageable.class))).thenReturn(emptyPage);

            mockMvc.perform(get("/api/activity-logs/user/{platformUserId}", targetUserId)
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
        }
    }

    // Access control tests (e.g. non-admin trying to access other user's logs) would depend
    // on how SecurityConfig and the controller/service methods are implemented.
    // If ServerActivityLogController.getLogsForUser has @PreAuthorize("hasAuthority('ADMIN') or #platformUserId == authentication.principal.id")
    // then specific tests for access denied scenarios would be valuable.
    // For now, we assume the service layer or security annotations handle this.
}
