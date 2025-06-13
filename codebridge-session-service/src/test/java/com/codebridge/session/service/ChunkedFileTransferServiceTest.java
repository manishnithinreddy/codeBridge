package com.codebridge.session.service;

import com.codebridge.session.dto.FileTransferRequest;
import com.codebridge.session.dto.FileTransferResponse;
import com.codebridge.session.dto.TransferDirection;
import com.codebridge.session.dto.TransferStatus;
import com.codebridge.session.exception.FileTransferException;
import com.codebridge.session.model.FileTransferRecord;
import com.codebridge.session.repository.FileTransferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ChunkedFileTransferServiceTest {

    @Mock
    private FileTransferRepository fileTransferRepository;
    
    @Mock
    private SessionService sessionService;
    
    @InjectMocks
    private ChunkedFileTransferService fileTransferService;
    
    private String tempDir;
    
    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);
        
        // Create a temporary directory for testing
        Path tempPath = Files.createTempDirectory("chunked_transfer_test");
        tempDir = tempPath.toString();
        
        // Set the temp directory in the service
        ReflectionTestUtils.setField(fileTransferService, "tempUploadDirectory", tempDir);
        ReflectionTestUtils.setField(fileTransferService, "defaultChunkSize", 1024);
        ReflectionTestUtils.setField(fileTransferService, "rateLimitBytesPerSecond", 0L); // No rate limiting for tests
        
        // Mock session service to return active session
        when(sessionService.isSessionActive(anyString())).thenReturn(true);
    }
    
    @Test
    void testInitializeUpload() {
        // Prepare test data
        FileTransferRequest request = FileTransferRequest.builder()
                .sessionId("test-session")
                .fileName("test-file.txt")
                .fileSize(2048L)
                .remotePath("/remote/path")
                .direction(TransferDirection.UPLOAD)
                .build();
        
        // Mock repository save
        when(fileTransferRepository.save(any(FileTransferRecord.class))).thenAnswer(invocation -> {
            FileTransferRecord record = invocation.getArgument(0);
            assertNotNull(record.getTransferId());
            assertEquals("test-session", record.getSessionId());
            assertEquals("test-file.txt", record.getFileName());
            assertEquals(2048L, record.getFileSize());
            assertEquals("/remote/path", record.getRemotePath());
            assertEquals(1024, record.getChunkSize());
            assertEquals(2, record.getTotalChunks());
            assertEquals(0, record.getCompletedChunks());
            assertEquals(TransferStatus.INITIALIZED, record.getStatus());
            assertNotNull(record.getCreatedAt());
            assertNotNull(record.getUpdatedAt());
            return record;
        });
        
        // Call the service method
        FileTransferResponse response = fileTransferService.initializeUpload(request);
        
        // Verify the response
        assertNotNull(response);
        assertNotNull(response.getTransferId());
        assertEquals("test-file.txt", response.getFileName());
        assertEquals(TransferStatus.INITIALIZED, response.getStatus());
        assertEquals(2, response.getTotalChunks());
        assertEquals(0, response.getCompletedChunks());
        assertEquals(0, response.getProgressPercentage());
        assertEquals("Upload initialized successfully", response.getMessage());
        
        // Verify repository was called
        verify(fileTransferRepository, times(1)).save(any(FileTransferRecord.class));
    }
    
    @Test
    void testInitializeUploadWithInvalidSession() {
        // Prepare test data
        FileTransferRequest request = FileTransferRequest.builder()
                .sessionId("invalid-session")
                .fileName("test-file.txt")
                .fileSize(2048L)
                .remotePath("/remote/path")
                .direction(TransferDirection.UPLOAD)
                .build();
        
        // Mock session service to return inactive session
        when(sessionService.isSessionActive("invalid-session")).thenReturn(false);
        
        // Call the service method and expect exception
        assertThrows(FileTransferException.class, () -> fileTransferService.initializeUpload(request));
        
        // Verify repository was not called
        verify(fileTransferRepository, never()).save(any(FileTransferRecord.class));
    }
    
    @Test
    void testUploadChunk() throws IOException {
        // Prepare test data
        String transferId = "test-transfer-id";
        int chunkNumber = 0;
        byte[] chunkData = "Test chunk data".getBytes();
        MockMultipartFile chunkFile = new MockMultipartFile("chunk", chunkData);
        
        // Create a transfer record
        FileTransferRecord record = new FileTransferRecord();
        record.setTransferId(transferId);
        record.setSessionId("test-session");
        record.setFileName("test-file.txt");
        record.setFileSize(2048L);
        record.setRemotePath("/remote/path");
        record.setChunkSize(1024);
        record.setTotalChunks(2);
        record.setCompletedChunks(0);
        record.setStatus(TransferStatus.INITIALIZED);
        record.setDirection(TransferDirection.UPLOAD);
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        
        // Mock repository findById
        when(fileTransferRepository.findById(transferId)).thenReturn(Optional.of(record));
        
        // Mock repository save
        when(fileTransferRepository.save(any(FileTransferRecord.class))).thenAnswer(invocation -> {
            FileTransferRecord savedRecord = invocation.getArgument(0);
            assertEquals(1, savedRecord.getCompletedChunks());
            assertEquals(TransferStatus.IN_PROGRESS, savedRecord.getStatus());
            return savedRecord;
        });
        
        // Call the service method
        FileTransferResponse response = fileTransferService.uploadChunk(transferId, chunkNumber, chunkFile);
        
        // Verify the response
        assertNotNull(response);
        assertEquals(transferId, response.getTransferId());
        assertEquals("test-file.txt", response.getFileName());
        assertEquals(TransferStatus.IN_PROGRESS, response.getStatus());
        assertEquals(2, response.getTotalChunks());
        assertEquals(1, response.getCompletedChunks());
        assertEquals(50, response.getProgressPercentage());
        assertEquals("Chunk uploaded successfully", response.getMessage());
        
        // Verify repository was called
        verify(fileTransferRepository, times(1)).findById(transferId);
        verify(fileTransferRepository, times(1)).save(any(FileTransferRecord.class));
        
        // Verify chunk file was created
        File chunkPath = new File(tempDir + File.separator + transferId + "_" + chunkNumber + ".part");
        assertTrue(chunkPath.exists());
        assertEquals(chunkData.length, chunkPath.length());
    }
    
    @Test
    void testUploadChunkWithInvalidTransferId() {
        // Prepare test data
        String transferId = "invalid-transfer-id";
        int chunkNumber = 0;
        byte[] chunkData = "Test chunk data".getBytes();
        MockMultipartFile chunkFile = new MockMultipartFile("chunk", chunkData);
        
        // Mock repository findById to return empty
        when(fileTransferRepository.findById(transferId)).thenReturn(Optional.empty());
        
        // Call the service method and expect exception
        assertThrows(FileTransferException.class, () -> fileTransferService.uploadChunk(transferId, chunkNumber, chunkFile));
        
        // Verify repository was called
        verify(fileTransferRepository, times(1)).findById(transferId);
        verify(fileTransferRepository, never()).save(any(FileTransferRecord.class));
    }
    
    @Test
    void testUploadChunkWithInvalidChunkNumber() {
        // Prepare test data
        String transferId = "test-transfer-id";
        int chunkNumber = 5; // Invalid chunk number
        byte[] chunkData = "Test chunk data".getBytes();
        MockMultipartFile chunkFile = new MockMultipartFile("chunk", chunkData);
        
        // Create a transfer record
        FileTransferRecord record = new FileTransferRecord();
        record.setTransferId(transferId);
        record.setSessionId("test-session");
        record.setFileName("test-file.txt");
        record.setFileSize(2048L);
        record.setRemotePath("/remote/path");
        record.setChunkSize(1024);
        record.setTotalChunks(2);
        record.setCompletedChunks(0);
        record.setStatus(TransferStatus.INITIALIZED);
        record.setDirection(TransferDirection.UPLOAD);
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        
        // Mock repository findById
        when(fileTransferRepository.findById(transferId)).thenReturn(Optional.of(record));
        
        // Call the service method and expect exception
        assertThrows(FileTransferException.class, () -> fileTransferService.uploadChunk(transferId, chunkNumber, chunkFile));
        
        // Verify repository was called
        verify(fileTransferRepository, times(1)).findById(transferId);
        verify(fileTransferRepository, never()).save(any(FileTransferRecord.class));
    }
    
    @Test
    void testRateLimiting() throws IOException {
        // Set a rate limit for testing
        ReflectionTestUtils.setField(fileTransferService, "rateLimitBytesPerSecond", 1024L); // 1KB/s
        
        // Prepare test data
        String transferId = "test-transfer-id";
        int chunkNumber = 0;
        byte[] chunkData = new byte[1024]; // 1KB of data
        MockMultipartFile chunkFile = new MockMultipartFile("chunk", chunkData);
        
        // Create a transfer record
        FileTransferRecord record = new FileTransferRecord();
        record.setTransferId(transferId);
        record.setSessionId("test-session");
        record.setFileName("test-file.txt");
        record.setFileSize(2048L);
        record.setRemotePath("/remote/path");
        record.setChunkSize(1024);
        record.setTotalChunks(2);
        record.setCompletedChunks(0);
        record.setStatus(TransferStatus.INITIALIZED);
        record.setDirection(TransferDirection.UPLOAD);
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        
        // Mock repository findById
        when(fileTransferRepository.findById(transferId)).thenReturn(Optional.of(record));
        
        // Mock repository save
        when(fileTransferRepository.save(any(FileTransferRecord.class))).thenReturn(record);
        
        // Measure time before and after to verify rate limiting
        long startTime = System.currentTimeMillis();
        
        // Call the service method
        FileTransferResponse response = fileTransferService.uploadChunk(transferId, chunkNumber, chunkFile);
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // With 1KB of data and a rate limit of 1KB/s, it should take at least 1000ms
        // But we'll check for at least 900ms to account for timing variations
        assertTrue(duration >= 900, "Rate limiting should delay the operation by at least 900ms");
        
        // Verify the response
        assertNotNull(response);
        assertEquals(transferId, response.getTransferId());
        assertEquals(TransferStatus.IN_PROGRESS, response.getStatus());
    }
}
