package com.codebridge.server.service;

import com.codebridge.server.dto.ServerResponse;
import com.codebridge.server.dto.remote.RemoteFileEntry;
import com.codebridge.server.exception.FileTransferException;
import com.codebridge.server.exception.ResourceNotFoundException;
import com.codebridge.server.model.SshKey;
import com.codebridge.server.model.Server;
import com.codebridge.server.model.SshKey;
import com.codebridge.server.model.enums.ServerAuthProvider;
// import com.codebridge.server.model.Server; // Not directly used
// import com.codebridge.server.model.SshKey; // Not directly used
// import com.codebridge.server.model.enums.ServerAuthProvider; // Not directly used
// import com.jcraft.jsch.ChannelSftp; // JSch logic moves to SessionService
// import com.jcraft.jsch.JSchException; // JSch logic moves to SessionService
// import com.jcraft.jsch.SftpATTRS; // JSch logic moves to SessionService
// import com.jcraft.jsch.SftpException; // JSch logic moves to SessionService
import com.codebridge.server.sessions.SessionKey;
// import com.codebridge.server.sessions.SessionManager; // Removed
// import com.codebridge.server.sessions.SshSessionWrapper; // Removed
import com.codebridge.server.exception.AccessDeniedException;
import com.codebridge.server.security.jwt.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile; // For type hint if needed, though not directly used
import org.springframework.web.util.UriComponentsBuilder;


import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
// import java.util.Vector; // No longer used
// import java.util.function.Supplier; // No longer used
// import java.nio.file.Paths; // No longer used for filename extraction here


@Service
public class FileTransferService {

    private static final Logger log = LoggerFactory.getLogger(FileTransferService.class);
    // Constants for JSch direct connection removed

    private final ServerAccessControlService serverAccessControlService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RestTemplate restTemplate;
    private final ServerActivityLogService activityLogService;

    @Value("${codebridge.service-urls.session-service}")
    private String sessionServiceBaseUrl;

    @Autowired
    public FileTransferService(ServerAccessControlService serverAccessControlService,
                               JwtTokenProvider jwtTokenProvider,
                               RestTemplate restTemplate,
                               ServerActivityLogService activityLogService) {
        this.serverAccessControlService = serverAccessControlService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.restTemplate = restTemplate;
        this.activityLogService = activityLogService;
    }

    private SessionKey validateTokenAndAuthorize(UUID serverId, String sessionToken, String operationName) throws FileTransferException {
        Optional<SessionKey> keyOpt = jwtTokenProvider.validateTokenAndExtractSessionKey(sessionToken);
        if (keyOpt.isEmpty()) {
            logActivity(serverId, null, operationName, "Invalid/expired session token.", "FAILURE", null);
            throw new AccessDeniedException("Invalid or expired session token for SFTP operation.");
        }
        SessionKey sessionKey = keyOpt.get();
        UUID platformUserId = sessionKey.userId();

        if (!sessionKey.resourceId().equals(serverId) || !"SSH".equals(sessionKey.resourceType())) {
            logActivity(serverId, platformUserId, operationName, "Token-server mismatch.", "FAILURE", null);
            throw new AccessDeniedException("Session token mismatch: Not valid for the requested server or resource type.");
        }

        try {
            serverAccessControlService.checkUserAccessAndGetConnectionDetails(platformUserId, serverId);
            log.debug("User {} authorized for server {} for SFTP operation '{}'", platformUserId, serverId, operationName);
        } catch (AccessDeniedException | ResourceNotFoundException e) {
            logActivity(serverId, platformUserId, operationName, "Authorization failed: " + e.getMessage(), "FAILURE", null);
            throw new FileTransferException("Authorization failed for SFTP operation: " + e.getMessage(), e);
        }
        return sessionKey;
    }

    public List<RemoteFileEntry> listFiles(UUID serverId, String sessionToken, String remotePath) throws FileTransferException {
        SessionKey sessionKey = validateTokenAndAuthorize(serverId, sessionToken, "SFTP_LIST_FILES");
        UUID platformUserId = sessionKey.userId();

        String url = UriComponentsBuilder.fromHttpUrl(sessionServiceBaseUrl + "/ops/ssh/" + sessionToken + "/sftp/list")
                .queryParam("remotePath", remotePath)
                .toUriString();
        HttpHeaders headers = new HttpHeaders();
        // Add inter-service auth if needed

        try {
            log.info("Delegating SFTP list for server {} (path: '{}', token: {}) to SessionService at {}", serverId, remotePath, sessionToken, url);
            ResponseEntity<List<RemoteFileEntry>> responseEntity = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<List<RemoteFileEntry>>() {}
            );
            logActivity(serverId, platformUserId, "SFTP_LIST_FILES", "Path: " + remotePath + ", Count: " + (responseEntity.getBody() != null ? responseEntity.getBody().size() : "N/A"), "SUCCESS", null);
            return responseEntity.getBody();
        } catch (HttpStatusCodeException e) {
            log.error("SessionService call failed for SFTP list (server {}): {} - {}", serverId, e.getStatusCode(), e.getResponseBodyAsString(), e);
            logActivity(serverId, platformUserId, "SFTP_LIST_FILES", "SessionService error: " + e.getStatusCode(), "FAILURE", e.getResponseBodyAsString());
            throw new FileTransferException("Failed to list files via SessionService: " + e.getStatusCode() + " " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("Unexpected error during SFTP list delegation (server {}): {}", serverId, e.getMessage(), e);
            logActivity(serverId, platformUserId, "SFTP_LIST_FILES", "Unexpected error: " + e.getMessage(), "FAILURE", null);
            throw new FileTransferException("Unexpected error delegating SFTP list: " + e.getMessage(), e);
        }
    }

    public byte[] downloadFile(UUID serverId, String sessionToken, String remotePath) throws FileTransferException {
        SessionKey sessionKey = validateTokenAndAuthorize(serverId, sessionToken, "SFTP_DOWNLOAD_FILE");
        UUID platformUserId = sessionKey.userId();

        String url = UriComponentsBuilder.fromHttpUrl(sessionServiceBaseUrl + "/ops/ssh/" + sessionToken + "/sftp/download")
                .queryParam("remotePath", remotePath)
                .toUriString();
        HttpHeaders headers = new HttpHeaders();

        try {
            log.info("Delegating SFTP download for server {} (path: '{}', token: {}) to SessionService at {}", serverId, remotePath, sessionToken, url);
            ResponseEntity<ByteArrayResource> responseEntity = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                ByteArrayResource.class
            );

            if (responseEntity.getBody() != null) {
                logActivity(serverId, platformUserId, "SFTP_DOWNLOAD_FILE", "Path: " + remotePath + ", Size: " + responseEntity.getBody().contentLength(), "SUCCESS", null);
                return responseEntity.getBody().getByteArray();
            } else {
                logActivity(serverId, platformUserId, "SFTP_DOWNLOAD_FILE", "Path: " + remotePath + ", SessionService returned empty body", "FAILURE", null);
                throw new FileTransferException("SessionService returned empty body for file download.");
            }
        } catch (HttpStatusCodeException e) {
            log.error("SessionService call failed for SFTP download (server {}): {} - {}", serverId, e.getStatusCode(), e.getResponseBodyAsString(), e);
            logActivity(serverId, platformUserId, "SFTP_DOWNLOAD_FILE", "SessionService error: " + e.getStatusCode(), "FAILURE", e.getResponseBodyAsString());
            throw new FileTransferException("Failed to download file via SessionService: " + e.getStatusCode() + " " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("Unexpected error during SFTP download delegation (server {}): {}", serverId, e.getMessage(), e);
            logActivity(serverId, platformUserId, "SFTP_DOWNLOAD_FILE", "Unexpected error: " + e.getMessage(), "FAILURE", null);
            throw new FileTransferException("Unexpected error delegating SFTP download: " + e.getMessage(), e);
        }
    }

    public void uploadFile(UUID serverId, String sessionToken, String remotePath, InputStream inputStream, String remoteFileName) throws FileTransferException {
        SessionKey sessionKey = validateTokenAndAuthorize(serverId, sessionToken, "SFTP_UPLOAD_FILE");
        UUID platformUserId = sessionKey.userId();

        // For file uploads with RestTemplate, typically use MultipartBodyBuilder or handle as Resource
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(inputStream.readAllBytes()) { // Read stream to bytes, consider streaming for large files
            @Override
            public String getFilename() {
                return remoteFileName; // Crucial for server side to get filename
            }
        });
        // remotePath is part of the URL for SessionService endpoint

        String url = UriComponentsBuilder.fromHttpUrl(sessionServiceBaseUrl + "/ops/ssh/" + sessionToken + "/sftp/upload")
                .queryParam("remotePath", remotePath) // SessionService's SshOperationController expects remotePath as @RequestParam
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        // Add inter-service auth if needed

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            log.info("Delegating SFTP upload for server {} (path: '{}', filename: '{}', token: {}) to SessionService at {}",
                     serverId, remotePath, remoteFileName, sessionToken, url);
            restTemplate.exchange(
                url,
                HttpMethod.POST,
                requestEntity,
                Void.class
            );
            logActivity(serverId, platformUserId, "SFTP_UPLOAD_FILE", "Path: " + remotePath + "/" + remoteFileName, "SUCCESS", null);
        } catch (HttpStatusCodeException e) {
            log.error("SessionService call failed for SFTP upload (server {}): {} - {}", serverId, e.getStatusCode(), e.getResponseBodyAsString(), e);
            logActivity(serverId, platformUserId, "SFTP_UPLOAD_FILE", "SessionService error: " + e.getStatusCode(), "FAILURE", e.getResponseBodyAsString());
            throw new FileTransferException("Failed to upload file via SessionService: " + e.getStatusCode() + " " + e.getResponseBodyAsString(), e);
        } catch (Exception e) { // Catches IOException from readAllBytes too
            log.error("Unexpected error during SFTP upload delegation (server {}): {}", serverId, e.getMessage(), e);
            logActivity(serverId, platformUserId, "SFTP_UPLOAD_FILE", "Unexpected error: " + e.getMessage(), "FAILURE", null);
            throw new FileTransferException("Unexpected error delegating SFTP upload: " + e.getMessage(), e);
        }
    }
}
