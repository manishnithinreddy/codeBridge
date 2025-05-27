package com.codebridge.docker.service;

import com.codebridge.docker.model.DockerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service for managing Docker contexts.
 */
@Service
public class DockerContextService {

    private static final Logger logger = LoggerFactory.getLogger(DockerContextService.class);

    // In-memory storage for demo purposes - in production, use a database
    private final List<DockerContext> contexts = new ArrayList<>();
    private final Map<String, Process> activeProcesses = new ConcurrentHashMap<>();

    /**
     * Get all Docker contexts.
     *
     * @return List of Docker contexts
     */
    public List<DockerContext> getAllContexts() {
        return Collections.unmodifiableList(contexts);
    }

    /**
     * Get a Docker context by ID.
     *
     * @param id The context ID
     * @return The Docker context
     */
    public DockerContext getContextById(String id) {
        return contexts.stream()
                .filter(context -> context.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    /**
     * Add a new Docker context.
     *
     * @param context The Docker context to add
     * @return The added Docker context
     */
    public DockerContext addContext(DockerContext context) {
        context.setId(UUID.randomUUID().toString());
        context.setCreatedAt(LocalDateTime.now());
        context.setUpdatedAt(LocalDateTime.now());
        contexts.add(context);
        return context;
    }

    /**
     * Update a Docker context.
     *
     * @param id The context ID
     * @param context The updated Docker context
     * @return The updated Docker context
     */
    public DockerContext updateContext(String id, DockerContext context) {
        DockerContext existingContext = getContextById(id);
        if (existingContext == null) {
            return null;
        }
        
        context.setId(id);
        context.setCreatedAt(existingContext.getCreatedAt());
        context.setUpdatedAt(LocalDateTime.now());
        
        contexts.remove(existingContext);
        contexts.add(context);
        
        return context;
    }

    /**
     * Delete a Docker context.
     *
     * @param id The context ID
     * @return True if deleted successfully
     */
    public boolean deleteContext(String id) {
        DockerContext context = getContextById(id);
        if (context == null) {
            return false;
        }
        
        return contexts.remove(context);
    }

    /**
     * Set a Docker context as default.
     *
     * @param id The context ID
     * @return The updated Docker context
     */
    public DockerContext setDefaultContext(String id) {
        DockerContext context = getContextById(id);
        if (context == null) {
            return null;
        }
        
        // Set all contexts as non-default
        contexts.forEach(c -> c.setDefault(false));
        
        // Set the specified context as default
        context.setDefault(true);
        
        return context;
    }

    /**
     * Test connection to a Docker context.
     *
     * @param context The Docker context to test
     * @return True if connection is successful
     */
    public boolean testContextConnection(DockerContext context) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            
            List<String> command = new ArrayList<>();
            command.add("docker");
            
            // Add context-specific environment variables
            if (context.getDockerHost() != null && !context.getDockerHost().isEmpty()) {
                processBuilder.environment().put("DOCKER_HOST", context.getDockerHost());
            }
            
            if (context.isTlsEnabled()) {
                processBuilder.environment().put("DOCKER_TLS_VERIFY", "1");
                
                if (context.getCertPath() != null && !context.getCertPath().isEmpty()) {
                    processBuilder.environment().put("DOCKER_CERT_PATH", context.getCertPath());
                }
            }
            
            command.add("info");
            command.add("--format");
            command.add("{{.ServerVersion}}");
            
            processBuilder.command(command);
            
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            
            return exitCode == 0;
        } catch (Exception e) {
            logger.error("Error testing connection to context {}: {}", context.getName(), e.getMessage());
            return false;
        }
    }

    /**
     * Execute a Docker command in a specific context.
     *
     * @param contextId The context ID
     * @param command The Docker command to execute
     * @return The command output
     */
    public String executeCommand(String contextId, List<String> command) {
        DockerContext context = getContextById(contextId);
        if (context == null) {
            return "Context not found";
        }
        
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            
            List<String> fullCommand = new ArrayList<>();
            fullCommand.add("docker");
            fullCommand.addAll(command);
            
            // Add context-specific environment variables
            if (context.getDockerHost() != null && !context.getDockerHost().isEmpty()) {
                processBuilder.environment().put("DOCKER_HOST", context.getDockerHost());
            }
            
            if (context.isTlsEnabled()) {
                processBuilder.environment().put("DOCKER_TLS_VERIFY", "1");
                
                if (context.getCertPath() != null && !context.getCertPath().isEmpty()) {
                    processBuilder.environment().put("DOCKER_CERT_PATH", context.getCertPath());
                }
            }
            
            processBuilder.command(fullCommand);
            
            Process process = processBuilder.start();
            activeProcesses.put(contextId, process);
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String output = reader.lines().collect(Collectors.joining("\n"));
            
            process.waitFor();
            activeProcesses.remove(contextId);
            
            return output;
        } catch (Exception e) {
            logger.error("Error executing command in context {}: {}", context.getName(), e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Stop a running Docker command.
     *
     * @param contextId The context ID
     * @return True if stopped successfully
     */
    public boolean stopCommand(String contextId) {
        Process process = activeProcesses.get(contextId);
        if (process == null) {
            return false;
        }
        
        process.destroy();
        activeProcesses.remove(contextId);
        
        return true;
    }
}

