package com.codebridge.server.service.notification;

import com.codebridge.server.dto.notification.NotificationMessage;
import com.codebridge.server.model.NotificationChannel;
import com.codebridge.server.model.SecurityEventType;
import com.codebridge.usermanagement.profile.model.NotificationPreference;
import com.codebridge.usermanagement.profile.service.NotificationPreferenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for sending security-related notifications to users.
 */
@Service
public class SecurityNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(SecurityNotificationService.class);

    private final NotificationPreferenceService notificationPreferenceService;
    private final NotificationDispatchService notificationDispatchService;

    @Autowired
    public SecurityNotificationService(
            NotificationPreferenceService notificationPreferenceService,
            NotificationDispatchService notificationDispatchService) {
        this.notificationPreferenceService = notificationPreferenceService;
        this.notificationDispatchService = notificationDispatchService;
    }

    /**
     * Sends a notification about a suspicious login attempt.
     *
     * @param userId the user ID
     * @param ipAddress the IP address of the login attempt
     * @param location the approximate location of the IP address
     * @param timestamp the timestamp of the login attempt
     * @param successful whether the login attempt was successful
     */
    public void notifySuspiciousLogin(UUID userId, String ipAddress, String location, 
                                     long timestamp, boolean successful) {
        String eventType = SecurityEventType.SUSPICIOUS_LOGIN.name();
        String title = successful ? "Suspicious Login Detected" : "Failed Login Attempt";
        
        Map<String, Object> details = new HashMap<>();
        details.put("ipAddress", ipAddress);
        details.put("location", location);
        details.put("timestamp", timestamp);
        details.put("successful", successful);
        
        String content = String.format(
                "A %s was detected from %s (%s) at %s.",
                successful ? "suspicious login" : "failed login attempt",
                ipAddress,
                location,
                new java.util.Date(timestamp)
        );
        
        sendSecurityNotification(userId, eventType, title, content, details);
    }

    /**
     * Sends a notification about unusual command execution.
     *
     * @param userId the user ID
     * @param serverId the server ID
     * @param command the command that was executed
     * @param ipAddress the IP address from which the command was executed
     * @param timestamp the timestamp of the command execution
     */
    public void notifyUnusualCommand(UUID userId, UUID serverId, String command, 
                                    String ipAddress, long timestamp) {
        String eventType = SecurityEventType.UNUSUAL_COMMAND.name();
        String title = "Unusual Command Execution Detected";
        
        Map<String, Object> details = new HashMap<>();
        details.put("serverId", serverId);
        details.put("command", command);
        details.put("ipAddress", ipAddress);
        details.put("timestamp", timestamp);
        
        String content = String.format(
                "An unusual command was executed on server %s from %s at %s: %s",
                serverId,
                ipAddress,
                new java.util.Date(timestamp),
                command
        );
        
        sendSecurityNotification(userId, eventType, title, content, details);
    }

    /**
     * Sends a notification about multiple failed login attempts.
     *
     * @param userId the user ID
     * @param count the number of failed attempts
     * @param ipAddresses the IP addresses of the failed attempts
     * @param timeWindow the time window in minutes
     */
    public void notifyMultipleFailedLogins(UUID userId, int count, 
                                          String[] ipAddresses, int timeWindow) {
        String eventType = SecurityEventType.MULTIPLE_FAILED_LOGINS.name();
        String title = "Multiple Failed Login Attempts";
        
        Map<String, Object> details = new HashMap<>();
        details.put("count", count);
        details.put("ipAddresses", ipAddresses);
        details.put("timeWindow", timeWindow);
        
        String content = String.format(
                "There were %d failed login attempts for your account in the last %d minutes. " +
                "IP addresses: %s",
                count,
                timeWindow,
                String.join(", ", ipAddresses)
        );
        
        sendSecurityNotification(userId, eventType, title, content, details);
    }

    /**
     * Sends a notification about a new location login.
     *
     * @param userId the user ID
     * @param ipAddress the IP address of the login
     * @param location the location of the login
     * @param timestamp the timestamp of the login
     */
    public void notifyNewLocationLogin(UUID userId, String ipAddress, 
                                      String location, long timestamp) {
        String eventType = SecurityEventType.NEW_LOCATION_LOGIN.name();
        String title = "Login from New Location";
        
        Map<String, Object> details = new HashMap<>();
        details.put("ipAddress", ipAddress);
        details.put("location", location);
        details.put("timestamp", timestamp);
        
        String content = String.format(
                "Your account was accessed from a new location: %s (%s) at %s.",
                location,
                ipAddress,
                new java.util.Date(timestamp)
        );
        
        sendSecurityNotification(userId, eventType, title, content, details);
    }

    /**
     * Sends a security notification to a user based on their notification preferences.
     *
     * @param userId the user ID
     * @param eventType the event type
     * @param title the notification title
     * @param content the notification content
     * @param details additional details
     */
    private void sendSecurityNotification(UUID userId, String eventType, String title, 
                                         String content, Map<String, Object> details) {
        try {
            // Get user's notification preferences for this event type
            NotificationPreference preference = 
                    notificationPreferenceService.getUserNotificationPreference(userId, eventType);
            
            // Create notification message
            NotificationMessage message = new NotificationMessage(
                    UUID.randomUUID(),
                    userId,
                    eventType,
                    title,
                    content,
                    details,
                    System.currentTimeMillis()
            );
            
            // Send notifications based on user preferences
            if (preference.isEmailEnabled()) {
                notificationDispatchService.sendNotification(message, NotificationChannel.EMAIL);
            }
            
            if (preference.isPushEnabled()) {
                notificationDispatchService.sendNotification(message, NotificationChannel.PUSH);
            }
            
            if (preference.isInAppEnabled()) {
                notificationDispatchService.sendNotification(message, NotificationChannel.IN_APP);
            }
            
            if (preference.isSlackEnabled()) {
                notificationDispatchService.sendNotification(message, NotificationChannel.SLACK);
            }
            
            // Handle custom channel if configured
            if (preference.getCustomChannel() != null && !preference.getCustomChannel().isEmpty()) {
                notificationDispatchService.sendNotificationToCustomChannel(
                        message, 
                        preference.getCustomChannel(), 
                        preference.getCustomChannelConfig()
                );
            }
            
            logger.info("Sent security notification of type {} to user {}", eventType, userId);
            
        } catch (Exception e) {
            logger.error("Failed to send security notification to user {}: {}", userId, e.getMessage(), e);
        }
    }
}

