package com.codebridge.monitoring.performance.collector;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Collector for SSH-related metrics.
 * This collector captures connection times, command execution times,
 * and success/failure rates for SSH operations.
 */
@Component
@Slf4j
public class SshMetricsCollector {

    private static final String SERVICE_NAME = "ssh-service";
    
    private final PerformanceMetricsCollector metricsCollector;
    private final ConcurrentHashMap<String, AtomicLong> hostConnectionCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> hostFailureCounts = new ConcurrentHashMap<>();
    
    @Value("${services.ssh.metrics-enabled:true}")
    private boolean sshMetricsEnabled;
    
    @Value("${services.ssh.connection-timeout:5000}")
    private long connectionTimeout;

    @Autowired
    public SshMetricsCollector(PerformanceMetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;
    }

    /**
     * Record SSH connection metrics.
     *
     * @param host the SSH host
     * @param port the SSH port
     * @param username the SSH username
     * @param durationMs the connection duration in milliseconds
     * @param success whether the connection was successful
     */
    public void recordConnection(String host, int port, String username, long durationMs, boolean success) {
        if (!sshMetricsEnabled) {
            return;
        }
        
        Map<String, String> tags = new HashMap<>();
        tags.put("host", host);
        tags.put("port", String.valueOf(port));
        tags.put("username", username);
        tags.put("success", String.valueOf(success));
        
        // Record connection time
        metricsCollector.recordTimer(SERVICE_NAME, "connection.time", durationMs, tags);
        
        // Increment connection counter
        metricsCollector.incrementCounter(SERVICE_NAME, "connection.count", tags);
        
        // Track host-specific metrics
        String hostKey = host + ":" + port;
        hostConnectionCounts.computeIfAbsent(hostKey, k -> new AtomicLong(0)).incrementAndGet();
        
        if (!success) {
            // Record connection failure
            metricsCollector.incrementCounter(SERVICE_NAME, "connection.failure", tags);
            
            // Track host-specific failure
            hostFailureCounts.computeIfAbsent(hostKey, k -> new AtomicLong(0)).incrementAndGet();
            
            // Record timeout if applicable
            if (durationMs >= connectionTimeout) {
                metricsCollector.incrementCounter(SERVICE_NAME, "connection.timeout", tags);
            }
        }
        
        // Calculate and record failure rate
        long connectionCount = hostConnectionCounts.get(hostKey).get();
        long failureCount = hostFailureCounts.getOrDefault(hostKey, new AtomicLong(0)).get();
        double failureRate = (double) failureCount / connectionCount;
        
        Map<String, String> failureRateTags = new HashMap<>();
        failureRateTags.put("host", host);
        failureRateTags.put("port", String.valueOf(port));
        metricsCollector.recordGauge(SERVICE_NAME, "connection.failure.rate", failureRate, failureRateTags);
    }

    /**
     * Record SSH command execution metrics.
     *
     * @param host the SSH host
     * @param command the command executed
     * @param durationMs the execution duration in milliseconds
     * @param exitCode the command exit code
     */
    public void recordCommandExecution(String host, String command, long durationMs, int exitCode) {
        if (!sshMetricsEnabled) {
            return;
        }
        
        Map<String, String> tags = new HashMap<>();
        tags.put("host", host);
        tags.put("command_type", getCommandType(command));
        tags.put("exit_code", String.valueOf(exitCode));
        tags.put("success", exitCode == 0 ? "true" : "false");
        
        // Record execution time
        metricsCollector.recordTimer(SERVICE_NAME, "command.execution.time", durationMs, tags);
        
        // Increment execution counter
        metricsCollector.incrementCounter(SERVICE_NAME, "command.execution.count", tags);
        
        if (exitCode != 0) {
            // Record execution failure
            metricsCollector.incrementCounter(SERVICE_NAME, "command.execution.failure", tags);
        }
    }

    /**
     * Record SSH file transfer metrics.
     *
     * @param host the SSH host
     * @param operation the operation (upload or download)
     * @param fileSize the file size in bytes
     * @param durationMs the transfer duration in milliseconds
     * @param success whether the transfer was successful
     */
    public void recordFileTransfer(String host, String operation, long fileSize, long durationMs, boolean success) {
        if (!sshMetricsEnabled) {
            return;
        }
        
        Map<String, String> tags = new HashMap<>();
        tags.put("host", host);
        tags.put("operation", operation);
        tags.put("success", String.valueOf(success));
        
        // Record transfer time
        metricsCollector.recordTimer(SERVICE_NAME, "file.transfer.time", durationMs, tags);
        
        // Record file size
        metricsCollector.recordGauge(SERVICE_NAME, "file.transfer.size", fileSize, tags);
        
        // Calculate and record transfer rate (bytes per second)
        double transferRate = durationMs > 0 ? (fileSize * 1000.0 / durationMs) : 0;
        metricsCollector.recordGauge(SERVICE_NAME, "file.transfer.rate", transferRate, tags);
        
        // Increment transfer counter
        metricsCollector.incrementCounter(SERVICE_NAME, "file.transfer.count", tags);
        
        if (!success) {
            // Record transfer failure
            metricsCollector.incrementCounter(SERVICE_NAME, "file.transfer.failure", tags);
        }
    }
    
    /**
     * Get the command type from the command string.
     *
     * @param command the command string
     * @return the command type
     */
    private String getCommandType(String command) {
        if (command == null || command.isEmpty()) {
            return "unknown";
        }
        
        String trimmedCommand = command.trim();
        
        // Extract the first word (the command itself)
        int spaceIndex = trimmedCommand.indexOf(' ');
        String firstWord = spaceIndex > 0 ? trimmedCommand.substring(0, spaceIndex) : trimmedCommand;
        
        // Check for common command types
        if (firstWord.equals("ls") || firstWord.equals("dir") || firstWord.equals("find")) {
            return "file_listing";
        } else if (firstWord.equals("cd") || firstWord.equals("pwd")) {
            return "directory_navigation";
        } else if (firstWord.equals("cp") || firstWord.equals("mv") || firstWord.equals("rm")) {
            return "file_operation";
        } else if (firstWord.equals("grep") || firstWord.equals("awk") || firstWord.equals("sed")) {
            return "text_processing";
        } else if (firstWord.equals("ps") || firstWord.equals("top") || firstWord.equals("kill")) {
            return "process_management";
        } else if (firstWord.equals("cat") || firstWord.equals("less") || firstWord.equals("more")) {
            return "file_viewing";
        } else if (firstWord.equals("chmod") || firstWord.equals("chown")) {
            return "permission_management";
        } else if (firstWord.equals("ssh") || firstWord.equals("scp") || firstWord.equals("sftp")) {
            return "remote_access";
        } else if (firstWord.equals("git")) {
            return "version_control";
        } else if (firstWord.equals("docker") || firstWord.equals("kubectl")) {
            return "container_management";
        } else if (firstWord.equals("npm") || firstWord.equals("yarn") || firstWord.equals("mvn")) {
            return "package_management";
        } else {
            return "other";
        }
    }
}

