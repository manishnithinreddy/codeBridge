package com.codebridge.project.web.rest;

import com.codebridge.project.security.SecurityUtils;
import com.codebridge.project.service.FileTransferService;
import com.codebridge.project.service.dto.RemoteFileEntry;
import com.codebridge.project.service.exception.AccessDeniedException;
import com.codebridge.project.service.exception.FileTransferException;
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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;


import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FileTransferController.class)
@Import(GlobalExceptionHandler.class)
class FileTransferControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FileTransferService fileTransferService;

    @Autowired
    private ObjectMapper objectMapper;

    private String mockUserId;
    private String serverId;

    @BeforeEach
    void setUp() {
        mockUserId = UUID.randomUUID().toString();
        serverId = UUID.randomUUID().toString();
    }

    @Test
    void listFiles_shouldReturnListOfFiles() throws Exception {
        String path = ".";
        RemoteFileEntry fileEntry = new RemoteFileEntry();
        fileEntry.setFilename("test.txt");
        fileEntry.setDirectory(false);
        fileEntry.setSize(1024L);
        List<RemoteFileEntry> fileList = Collections.singletonList(fileEntry);

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.of(mockUserId));
            when(fileTransferService.listFiles(eq(mockUserId), eq(serverId), eq(path))).thenReturn(fileList);

            mockMvc.perform(get("/api/servers/{serverId}/files/list", serverId)
                    .param("path", path)
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].filename").value("test.txt"))
                .andExpect(jsonPath("$[0].directory").value(false));
        }
    }

    @Test
    void listFiles_whenPathIsMissing_shouldDefaultAndSucceed() throws Exception {
        // Assuming controller defaults path or service handles null/empty path gracefully
        RemoteFileEntry fileEntry = new RemoteFileEntry();
        fileEntry.setFilename("default.txt");
        List<RemoteFileEntry> fileList = Collections.singletonList(fileEntry);

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.of(mockUserId));
            // Path will be null if not provided, ensure service mock handles this
            when(fileTransferService.listFiles(eq(mockUserId), eq(serverId), any())).thenReturn(fileList);


            mockMvc.perform(get("/api/servers/{serverId}/files/list", serverId)
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].filename").value("default.txt"));
        }
    }


    @Test
    void listFiles_whenServerNotFound_shouldReturnNotFound() throws Exception {
        String path = ".";
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.of(mockUserId));
            when(fileTransferService.listFiles(eq(mockUserId), eq(serverId), eq(path)))
                .thenThrow(new ResourceNotFoundException("Server not found"));

            mockMvc.perform(get("/api/servers/{serverId}/files/list", serverId)
                    .param("path", path)
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Server not found"));
        }
    }

    @Test
    void listFiles_whenAccessDenied_shouldReturnForbidden() throws Exception {
        String path = ".";
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.of(mockUserId));
            when(fileTransferService.listFiles(eq(mockUserId), eq(serverId), eq(path)))
                .thenThrow(new AccessDeniedException("Access denied"));

            mockMvc.perform(get("/api/servers/{serverId}/files/list", serverId)
                    .param("path", path)
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"));
        }
    }

    @Test
    void listFiles_whenFileTransferException_shouldReturnInternalServerError() throws Exception {
        String path = ".";
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.of(mockUserId));
            when(fileTransferService.listFiles(eq(mockUserId), eq(serverId), eq(path)))
                .thenThrow(new FileTransferException("SFTP error"));

            mockMvc.perform(get("/api/servers/{serverId}/files/list", serverId)
                    .param("path", path)
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("SFTP error"));
        }
    }


    @Test
    void downloadFile_shouldReturnFile() throws Exception {
        String filePath = "/path/to/remote/file.txt";
        byte[] content = "File content".getBytes();
        Resource resource = new ByteArrayResource(content);

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.of(mockUserId));
            when(fileTransferService.downloadFile(eq(mockUserId), eq(serverId), eq(filePath))).thenReturn(resource);

            MvcResult result = mockMvc.perform(get("/api/servers/{serverId}/files/download", serverId)
                    .param("filePath", filePath))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"file.txt\""))
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andReturn();

            assertThat(result.getResponse().getContentAsByteArray()).isEqualTo(content);
        }
    }

    @Test
    void downloadFile_whenFilePathIsMissing_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/api/servers/{serverId}/files/download", serverId))
            .andExpect(status().isBadRequest()); // Based on @RequestParam(required=true)
    }


    @Test
    void downloadFile_whenFileNotFound_shouldReturnNotFound() throws Exception {
        String filePath = "/path/to/nonexistent/file.txt";
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.of(mockUserId));
            when(fileTransferService.downloadFile(eq(mockUserId), eq(serverId), eq(filePath)))
                .thenThrow(new ResourceNotFoundException("File not found on server"));

            mockMvc.perform(get("/api/servers/{serverId}/files/download", serverId)
                    .param("filePath", filePath))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("File not found on server"));
        }
    }

    @Test
    void downloadFile_whenFileTransferException_shouldReturnInternalServerError() throws Exception {
        String filePath = "/path/to/remote/file.txt";
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.of(mockUserId));
            when(fileTransferService.downloadFile(eq(mockUserId), eq(serverId), eq(filePath)))
                .thenThrow(new FileTransferException("Cannot download file"));

            mockMvc.perform(get("/api/servers/{serverId}/files/download", serverId)
                    .param("filePath", filePath))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Cannot download file"));
        }
    }


    @Test
    void uploadFile_shouldReturnOk() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "upload.txt", MediaType.TEXT_PLAIN_VALUE, "Upload content".getBytes());
        String remotePath = "/remote/path/";

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.of(mockUserId));
            // doNothing() for void methods
            // doNothing().when(fileTransferService).uploadFile(eq(mockUserId), eq(serverId), eq(remotePath), eq(file.getOriginalFilename()), any(InputStream.class), eq(file.getSize()));


            mockMvc.perform(MockMvcRequestBuilders.multipart("/api/servers/{serverId}/files/upload", serverId)
                    .file(file)
                    .param("remotePath", remotePath)
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(content().string("File uploaded successfully to " + remotePath + file.getOriginalFilename()));
        }
    }

    @Test
    void uploadFile_whenFileIsEmpty_shouldReturnBadRequest() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "", MediaType.TEXT_PLAIN_VALUE, new byte[0]);
        String remotePath = "/remote/path/";

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/servers/{serverId}/files/upload", serverId)
                .file(emptyFile)
                .param("remotePath", remotePath)
                .contentType(MediaType.MULTIPART_FORM_DATA))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Uploaded file cannot be empty."));
    }

    @Test
    void uploadFile_whenRemotePathIsMissing_shouldUseDefaultAndSucceed() throws Exception {
        // Assuming controller defaults remotePath or service handles null/empty path
        MockMultipartFile file = new MockMultipartFile("file", "upload.txt", MediaType.TEXT_PLAIN_VALUE, "Upload content".getBytes());
        String expectedDefaultPath = "./"; // Example default

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.of(mockUserId));
            // Service mock should expect the default path if controller sets one, or handle null
            // doNothing().when(fileTransferService).uploadFile(eq(mockUserId), eq(serverId), eq(expectedDefaultPath), ...);

            mockMvc.perform(MockMvcRequestBuilders.multipart("/api/servers/{serverId}/files/upload", serverId)
                    .file(file)
                    // No remotePath param
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(content().string("File uploaded successfully to " + expectedDefaultPath + file.getOriginalFilename()));
        }
    }


    @Test
    void uploadFile_whenFileTransferException_shouldReturnInternalServerError() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "upload.txt", MediaType.TEXT_PLAIN_VALUE, "Upload content".getBytes());
        String remotePath = "/remote/path/";

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.of(mockUserId));
            when(fileTransferService.uploadFile(eq(mockUserId), eq(serverId), eq(remotePath), eq(file.getOriginalFilename()), any(), eq(file.getSize())))
                 .thenThrow(new FileTransferException("Failed to upload file"));


            mockMvc.perform(MockMvcRequestBuilders.multipart("/api/servers/{serverId}/files/upload", serverId)
                    .file(file)
                    .param("remotePath", remotePath)
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Failed to upload file"));
        }
    }
}
