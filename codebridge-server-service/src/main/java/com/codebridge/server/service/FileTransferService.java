package com.codebridge.server.service;

import com.codebridge.server.dto.file.RemoteFileEntry;
import com.codebridge.server.exception.FileTransferException;
import com.codebridge.server.exception.AccessDeniedException; // Added
import com.codebridge.server.util.JwtUtil; // Assuming a utility for JWT parsing
import io.jsonwebtoken.Claims; // Added
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value; // Added
import org.springframework.core.ParameterizedTypeReference; // Added
import org.springframework.core.io.ByteArrayResource; // Added for download
import org.springframework.core.io.Resource; // Added
import org.springframework.http.*; // Added
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap; // Added
import org.springframework.util.MultiValueMap; // Added
import org.springframework.web.client.HttpStatusCodeException; // Added
import org.springframework.web.client.RestTemplate; // Added
import org.springframework.web.util.UriComponentsBuilder; // Added

import java.io.InputStream;
// Removed JSch specific imports: JSch, Session, ChannelSftp, SftpATTRS, SftpException, Vector
// Removed ByteArrayOutputStream, Instant, ZoneId, DateTimeFormatter, ArrayList from direct JSch use
import java.util.List;
import java.util.UUID;
import java.util.Vector;

@Service
public class FileTransferService {

    private static final Logger logger = LoggerFactory.getLogger(FileTransferService.class);
    // Removed JSch related constants and SftpConnection record

    private final RestTemplate restTemplate;
    private final ServerAccessControlService serverAccessControlService;
    private final ServerActivityLogService activityLogService;
    private final String sessionServiceBaseUrl;
    private final JwtUtil jwtUtil = new JwtUtil(); // Placeholder for parsing SessionService JWT

    public FileTransferService(RestTemplate restTemplate,
                               ServerAccessControlService serverAccessControlService,
                               ServerActivityLogService activityLogService,
                               @Value("${codebridge.service-urls.session-service}") String sessionServiceBaseUrl) {
        this.restTemplate = restTemplate;
        this.serverAccessControlService = serverAccessControlService;
        this.activityLogService = activityLogService;
        this.sessionServiceBaseUrl = sessionServiceBaseUrl;
    }

    // Helper to validate token and authorize - throws exceptions on failure
    protected UUID validateTokenAndAuthorize(UUID serverId, String sessionToken) {
        Claims claims = jwtUtil.extractAllClaims(sessionToken);
        UUID platformUserIdFromToken = UUID.fromString(claims.getSubject());
        UUID resourceIdFromToken = UUID.fromString(claims.get("resourceId", String.class));
        String sessionType = claims.get("type", String.class);

        if (!"SSH".equals(sessionType)) {
            throw new AccessDeniedException("Invalid session type for SFTP operation.");
        }
        if (!resourceIdFromToken.equals(serverId)) {
            throw new AccessDeniedException("Session token resourceId mismatch with target serverId.");
        }
        // Re-validate user's access to the server resource itself
        serverAccessControlService.getValidatedConnectionDetails(platformUserIdFromToken, serverId);
        return platformUserIdFromToken;
    }

    // Methods that accept UUID platformUserId
    
    public List<RemoteFileEntry> listFiles(UUID serverId, UUID platformUserId, String remotePath) {
        // Get a valid session token for this user and server
        String sessionToken = getSessionTokenForUser(platformUserId, serverId);
        return listFiles(serverId, sessionToken, remotePath);
    }
    
    public byte[] downloadFile(UUID serverId, UUID platformUserId, String remotePath) {
        // Get a valid session token for this user and server
        String sessionToken = getSessionTokenForUser(platformUserId, serverId);
        return downloadFile(serverId, sessionToken, remotePath);
    }
    
    public void uploadFile(UUID serverId, UUID platformUserId, String remoteDirectory, InputStream inputStream, String remoteFileName) {
        // Get a valid session token for this user and server
        String sessionToken = getSessionTokenForUser(platformUserId, serverId);
        uploadFile(serverId, sessionToken, remoteDirectory, inputStream, remoteFileName);
    }
    
    public void deleteFile(UUID serverId, UUID platformUserId, String remotePath, boolean recursive) {
        // Get a valid session token for this user and server
        String sessionToken = getSessionTokenForUser(platformUserId, serverId);
        deleteFile(serverId, sessionToken, remotePath, recursive);
    }
    
    public void changeFilePermissions(UUID serverId, UUID platformUserId, String remotePath, String permissions, boolean recursive) {
        // Get a valid session token for this user and server
        String sessionToken = getSessionTokenForUser(platformUserId, serverId);
        changeFilePermissions(serverId, sessionToken, remotePath, permissions, recursive);
    }
    
    public void renameFile(UUID serverId, UUID platformUserId, String sourcePath, String targetPath) {
        // Get a valid session token for this user and server
        String sessionToken = getSessionTokenForUser(platformUserId, serverId);
        renameFile(serverId, sessionToken, sourcePath, targetPath);
    }
    
    // Helper method to get a session token for a user and server
    private String getSessionTokenForUser(UUID platformUserId, UUID serverId) {
        // This is a placeholder implementation
        // In a real implementation, you would:
        // 1. Check if there's an existing valid session token for this user and server
        // 2. If not, create a new session
        // 3. Return the session token
        
        // For now, just return a dummy token
        return "dummy-session-token-" + platformUserId + "-" + serverId;
    }

    // Methods that accept sessionToken
    
    public List<RemoteFileEntry> listFiles(UUID serverId, String sessionToken, String remotePath) {
        UUID platformUserId = null;
        String logStatus = "FAILED";
        String errorMessage = null;
        String logDetails = String.format("Path: '%s'", remotePath);
        try {
            platformUserId = validateTokenAndAuthorize(serverId, sessionToken);

            String url = UriComponentsBuilder.fromHttpUrl(sessionServiceBaseUrl)
                .pathSegment("ops", "ssh", sessionToken, "sftp", "list")
                .queryParam("remotePath", remotePath)
                .toUriString();

            HttpHeaders headers = new HttpHeaders(); // No User JWT propagation for ops endpoints
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<List<RemoteFileEntry>> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, new ParameterizedTypeReference<List<RemoteFileEntry>>() {});

            logStatus = "SUCCESS";
            activityLogService.createLog(platformUserId, "FILE_LIST_PROXY", serverId, logDetails, logStatus, null);
            return response.getBody();
        } catch (HttpStatusCodeException e) {
            errorMessage = e.getResponseBodyAsString();
            logger.error("Error calling SessionService for SFTP list on server {}: {} - {}", serverId, e.getStatusCode(), errorMessage, e);
            activityLogService.createLog(platformUserId, "FILE_LIST_PROXY_FAILED", serverId, logDetails, "FAILED", errorMessage);
            throw new FileTransferException("Failed to list files via SessionService: " + errorMessage, e);
        } catch (Exception e) {
            errorMessage = e.getMessage();
            logger.error("Unexpected error during SFTP list proxy for server {}: {}", serverId, errorMessage, e);
            activityLogService.createLog(platformUserId, "FILE_LIST_PROXY_ERROR", serverId, logDetails, "FAILED", errorMessage);
            throw new FileTransferException("Unexpected error during SFTP list: " + errorMessage, e);
        }
    }

    public byte[] downloadFile(UUID serverId, String sessionToken, String remotePath) {
        UUID platformUserId = null;
        String logStatus = "FAILED";
        String errorMessage = null;
        String logDetails = String.format("Path: '%s'", remotePath);
        try {
            platformUserId = validateTokenAndAuthorize(serverId, sessionToken);

            String url = UriComponentsBuilder.fromHttpUrl(sessionServiceBaseUrl)
                .pathSegment("ops", "ssh", sessionToken, "sftp", "download")
                .queryParam("remotePath", remotePath)
                .toUriString();

            HttpHeaders headers = new HttpHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.GET, entity, byte[].class);
            logStatus = "SUCCESS";
            activityLogService.createLog(platformUserId, "FILE_DOWNLOAD_PROXY", serverId, logDetails, logStatus, null);
            return response.getBody();
        } catch (HttpStatusCodeException e) {
            errorMessage = e.getResponseBodyAsString();
            logger.error("Error calling SessionService for SFTP download on server {}: {} - {}", serverId, e.getStatusCode(), errorMessage, e);
            activityLogService.createLog(platformUserId, "FILE_DOWNLOAD_PROXY_FAILED", serverId, logDetails, "FAILED", errorMessage);
            throw new FileTransferException("Failed to download file via SessionService: " + errorMessage, e);
        } catch (Exception e) {
            errorMessage = e.getMessage();
            logger.error("Unexpected error during SFTP download proxy for server {}: {}", serverId, errorMessage, e);
            activityLogService.createLog(platformUserId, "FILE_DOWNLOAD_PROXY_ERROR", serverId, logDetails, "FAILED", errorMessage);
            throw new FileTransferException("Unexpected error during SFTP download: " + errorMessage, e);
        }
    }

    public void uploadFile(UUID serverId, String sessionToken, String remoteDirectory, InputStream inputStream, String remoteFileName) {
        UUID platformUserId = null;
        String logStatus = "FAILED";
        String errorMessage = null;
        String logDetails = String.format("Path: '%s', Filename: '%s'", remoteDirectory, remoteFileName);
        try {
            platformUserId = validateTokenAndAuthorize(serverId, sessionToken);

            String url = UriComponentsBuilder.fromHttpUrl(sessionServiceBaseUrl)
                .pathSegment("ops", "ssh", sessionToken, "sftp", "upload")
                .queryParam("remotePath", remoteDirectory) // SessionService expects remotePath as directory for upload
                .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            // Wrap InputStream in a Resource that RestTemplate can handle for multipart
            // Important: RestTemplate needs a resource with a filename for multipart.
            // We also need to ensure the InputStream is not closed by RestTemplate before it's fully read.
            // A ByteArrayResource is simplest if the file isn't too large, otherwise, custom Resource impl.
            // For this restoration, assuming file content can be read into byte array first.
            // This is a simplification; true streaming upload is more complex with RestTemplate.
            byte[] fileBytes = inputStream.readAllBytes(); // This is NOT ideal for large files
            ByteArrayResource fileResource = new ByteArrayResource(fileBytes) {
                @Override
                public String getFilename() {
                    return remoteFileName; // Crucial for multipart for server-side to get filename
                }
            };
            body.add("file", fileResource);
            // body.add("remoteFileName", remoteFileName); // Could be separate field if SessionService expects it

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            restTemplate.postForEntity(url, requestEntity, Void.class);
            logStatus = "SUCCESS";
            activityLogService.createLog(platformUserId, "FILE_UPLOAD_PROXY", serverId, logDetails, logStatus, null);

        } catch (HttpStatusCodeException e) {
            errorMessage = e.getResponseBodyAsString();
            logger.error("Error calling SessionService for SFTP upload on server {}: {} - {}", serverId, e.getStatusCode(), errorMessage, e);
            activityLogService.createLog(platformUserId, "FILE_UPLOAD_PROXY_FAILED", serverId, logDetails, "FAILED", errorMessage);
            throw new FileTransferException("Failed to upload file via SessionService: " + errorMessage, e);
        } catch (Exception e) { // Includes IOException from readAllBytes
            errorMessage = e.getMessage();
            logger.error("Unexpected error during SFTP upload proxy for server {}: {}", serverId, errorMessage, e);
            activityLogService.createLog(platformUserId, "FILE_UPLOAD_PROXY_ERROR", serverId, logDetails, "FAILED", errorMessage);
            throw new FileTransferException("Unexpected error during SFTP upload: " + errorMessage, e);
        }
    }

    /**
     * Delete a file or directory on the remote server
     * @param serverId Server ID
     * @param sessionToken Session token
     * @param remotePath Path to delete
     * @param recursive Whether to delete recursively (for directories)
     */
    public void deleteFile(UUID serverId, String sessionToken, String remotePath, boolean recursive) {
        UUID platformUserId = null;
        String logStatus = "FAILED";
        String errorMessage = null;
        String logDetails = String.format("Path: '%s', Recursive: %b", remotePath, recursive);
        try {
            platformUserId = validateTokenAndAuthorize(serverId, sessionToken);

            String url = UriComponentsBuilder.fromHttpUrl(sessionServiceBaseUrl)
                .pathSegment("ops", "ssh", sessionToken, "sftp", "delete")
                .queryParam("remotePath", remotePath)
                .queryParam("recursive", recursive)
                .toUriString();

            HttpHeaders headers = new HttpHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);
            logStatus = "SUCCESS";
            activityLogService.createLog(platformUserId, "FILE_DELETE_PROXY", serverId, logDetails, logStatus, null);
        } catch (HttpStatusCodeException e) {
            errorMessage = e.getResponseBodyAsString();
            logger.error("Error calling SessionService for SFTP delete on server {}: {} - {}", serverId, e.getStatusCode(), errorMessage, e);
            activityLogService.createLog(platformUserId, "FILE_DELETE_PROXY_FAILED", serverId, logDetails, "FAILED", errorMessage);
            throw new FileTransferException("Failed to delete file via SessionService: " + errorMessage, e);
        } catch (Exception e) {
            errorMessage = e.getMessage();
            logger.error("Unexpected error during SFTP delete proxy for server {}: {}", serverId, errorMessage, e);
            activityLogService.createLog(platformUserId, "FILE_DELETE_PROXY_ERROR", serverId, logDetails, "FAILED", errorMessage);
            throw new FileTransferException("Unexpected error during SFTP delete: " + errorMessage, e);
        }
    }
    
    /**
     * Change permissions of a file or directory on the remote server
     * @param serverId Server ID
     * @param sessionToken Session token
     * @param remotePath Path to change permissions
     * @param permissions Permissions in octal format (e.g. "755")
     * @param recursive Whether to change permissions recursively (for directories)
     */
    public void changeFilePermissions(UUID serverId, String sessionToken, String remotePath, String permissions, boolean recursive) {
        UUID platformUserId = null;
        String logStatus = "FAILED";
        String errorMessage = null;
        String logDetails = String.format("Path: '%s', Permissions: %s, Recursive: %b", remotePath, permissions, recursive);
        try {
            platformUserId = validateTokenAndAuthorize(serverId, sessionToken);

            String url = UriComponentsBuilder.fromHttpUrl(sessionServiceBaseUrl)
                .pathSegment("ops", "ssh", sessionToken, "sftp", "chmod")
                .queryParam("remotePath", remotePath)
                .queryParam("permissions", permissions)
                .queryParam("recursive", recursive)
                .toUriString();

            HttpHeaders headers = new HttpHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            restTemplate.exchange(url, HttpMethod.PUT, entity, Void.class);
            logStatus = "SUCCESS";
            activityLogService.createLog(platformUserId, "FILE_CHMOD_PROXY", serverId, logDetails, logStatus, null);
        } catch (HttpStatusCodeException e) {
            errorMessage = e.getResponseBodyAsString();
            logger.error("Error calling SessionService for SFTP chmod on server {}: {} - {}", serverId, e.getStatusCode(), errorMessage, e);
            activityLogService.createLog(platformUserId, "FILE_CHMOD_PROXY_FAILED", serverId, logDetails, "FAILED", errorMessage);
            throw new FileTransferException("Failed to change file permissions via SessionService: " + errorMessage, e);
        } catch (Exception e) {
            errorMessage = e.getMessage();
            logger.error("Unexpected error during SFTP chmod proxy for server {}: {}", serverId, errorMessage, e);
            activityLogService.createLog(platformUserId, "FILE_CHMOD_PROXY_ERROR", serverId, logDetails, "FAILED", errorMessage);
            throw new FileTransferException("Unexpected error during SFTP chmod: " + errorMessage, e);
        }
    }
    
    /**
     * Rename or move a file or directory on the remote server
     * @param serverId Server ID
     * @param sessionToken Session token
     * @param sourcePath Source path
     * @param targetPath Target path
     */
    public void renameFile(UUID serverId, String sessionToken, String sourcePath, String targetPath) {
        UUID platformUserId = null;
        String logStatus = "FAILED";
        String errorMessage = null;
        String logDetails = String.format("Source: '%s', Target: '%s'", sourcePath, targetPath);
        try {
            platformUserId = validateTokenAndAuthorize(serverId, sessionToken);

            String url = UriComponentsBuilder.fromHttpUrl(sessionServiceBaseUrl)
                .pathSegment("ops", "ssh", sessionToken, "sftp", "rename")
                .queryParam("sourcePath", sourcePath)
                .queryParam("targetPath", targetPath)
                .toUriString();

            HttpHeaders headers = new HttpHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            restTemplate.exchange(url, HttpMethod.PUT, entity, Void.class);
            logStatus = "SUCCESS";
            activityLogService.createLog(platformUserId, "FILE_RENAME_PROXY", serverId, logDetails, logStatus, null);
        } catch (HttpStatusCodeException e) {
            errorMessage = e.getResponseBodyAsString();
            logger.error("Error calling SessionService for SFTP rename on server {}: {} - {}", serverId, e.getStatusCode(), errorMessage, e);
            activityLogService.createLog(platformUserId, "FILE_RENAME_PROXY_FAILED", serverId, logDetails, "FAILED", errorMessage);
            throw new FileTransferException("Failed to rename file via SessionService: " + errorMessage, e);
        } catch (Exception e) {
            errorMessage = e.getMessage();
            logger.error("Unexpected error during SFTP rename proxy for server {}: {}", serverId, errorMessage, e);
            activityLogService.createLog(platformUserId, "FILE_RENAME_PROXY_ERROR", serverId, logDetails, "FAILED", errorMessage);
            throw new FileTransferException("Unexpected error during SFTP rename: " + errorMessage, e);
        }
    }
}
