package com.codebridge.session.controller;

import com.codebridge.session.service.command.SshCommandQueue;
import com.codebridge.session.service.connection.SshConnectionPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for health check and monitoring endpoints.
 * These endpoints provide visibility into the system's health and resource usage.
 */
@RestController
@RequestMapping("/api/health")
public class HealthCheckController {

    private final SshConnectionPool connectionPool;
    private final SshCommandQueue commandQueue;

    @Autowired
    public HealthCheckController(SshConnectionPool connectionPool, SshCommandQueue commandQueue) {
        this.connectionPool = connectionPool;
        this.commandQueue = commandQueue;
    }

    /**
     * Basic health check endpoint that returns the status of the service.
     *
     * @return A simple health status response
     */
    @GetMapping
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "session-service");
        return ResponseEntity.ok(response);
    }

    /**
     * Detailed health check that includes resource usage information.
     *
     * @return Detailed health information including connection and command queue metrics
     */
    @GetMapping("/details")
    public ResponseEntity<Map<String, Object>> detailedHealthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "session-service");
        
        // Add connection pool metrics
        Map<String, Object> connectionMetrics = new HashMap<>();
        connectionMetrics.put("activeConnections", connectionPool.getActiveConnectionCount());
        connectionMetrics.put("maxConnections", connectionPool.getMaxConnectionsPerInstance());
        connectionMetrics.put("availableConnections", connectionPool.getAvailableConnections());
        connectionMetrics.put("utilizationPercent", 
                Math.round((float) connectionPool.getActiveConnectionCount() / connectionPool.getMaxConnectionsPerInstance() * 100));
        response.put("connectionPool", connectionMetrics);
        
        // Add command queue metrics
        Map<String, Object> commandMetrics = new HashMap<>();
        commandMetrics.put("queuedCommands", commandQueue.getQueueSize());
        commandMetrics.put("activeCommands", commandQueue.getActiveCommandCount());
        response.put("commandQueue", commandMetrics);
        
        // Add JVM metrics
        Map<String, Object> jvmMetrics = new HashMap<>();
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long allocatedMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        jvmMetrics.put("maxMemoryMB", maxMemory / (1024 * 1024));
        jvmMetrics.put("allocatedMemoryMB", allocatedMemory / (1024 * 1024));
        jvmMetrics.put("freeMemoryMB", freeMemory / (1024 * 1024));
        jvmMetrics.put("availableProcessors", runtime.availableProcessors());
        response.put("jvm", jvmMetrics);
        
        return ResponseEntity.ok(response);
    }
}

