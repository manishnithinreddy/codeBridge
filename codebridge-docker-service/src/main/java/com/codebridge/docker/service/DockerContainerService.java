package com.codebridge.docker.service;

import com.codebridge.docker.model.DockerContainer;
import com.codebridge.docker.model.DockerContext;
import com.codebridge.docker.model.DockerLog;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
 * Service for managing Docker containers.
 */
@Service
public class DockerContainerService {

    private static final Logger logger = LoggerFactory.getLogger(DockerContainerService.class);

    private final DockerContextService contextService;
    private final ObjectMapper objectMapper;
    
    // In-memory storage for demo purposes - in production, use a database
    private final Map<String, Process> logStreams = new ConcurrentHashMap<>();

    @Autowired
    public DockerContainerService(DockerContextService contextService, ObjectMapper objectMapper) {
        this.contextService = contextService;
        this.objectMapper = objectMapper;
    }

    /**
     * Get all containers in a Docker context.
     *
     * @param contextId The context ID
     * @param showAll Whether to show all containers (including stopped ones)
     * @return List of Docker containers
     */
    public List<DockerContainer> getContainers(String contextId, boolean showAll) {
        DockerContext context = contextService.getContextById(contextId);
        if (context == null) {
            return Collections.emptyList();
        }
        
        try {
            List<String> command = new ArrayList<>();
            command.add("container");
            command.add("ls");
            command.add("--format");
            command.add("{{json .}}");
            
            if (showAll) {
                command.add("--all");
            }
            
            String output = contextService.executeCommand(contextId, command);
            
            List<DockerContainer> containers = new ArrayList<>();
            for (String line : output.split("\n")) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                
                Map<String, Object> containerData = objectMapper.readValue(line, Map.class);
                DockerContainer container = new DockerContainer();
                container.setId(UUID.randomUUID().toString());
                container.setContainerId((String) containerData.get("ID"));
                container.setName((String) containerData.get("Names"));
                container.setImageName((String) containerData.get("Image"));
                container.setStatus((String) containerData.get("Status"));
                container.setState(getContainerState((String) containerData.get("Status")));
                container.setCommand((String) containerData.get("Command"));
                container.setPorts(Collections.singletonList((String) containerData.get("Ports")));
                
                containers.add(container);
            }
            
            return containers;
        } catch (Exception e) {
            logger.error("Error fetching containers from context {}: {}", context.getName(), e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Get a container by ID.
     *
     * @param contextId The context ID
     * @param containerId The container ID
     * @return The Docker container
     */
    public DockerContainer getContainer(String contextId, String containerId) {
        DockerContext context = contextService.getContextById(contextId);
        if (context == null) {
            return null;
        }
        
        try {
            List<String> command = new ArrayList<>();
            command.add("container");
            command.add("inspect");
            command.add(containerId);
            
            String output = contextService.executeCommand(contextId, command);
            
            List<Map<String, Object>> containerData = objectMapper.readValue(output, List.class);
            if (containerData.isEmpty()) {
                return null;
            }
            
            Map<String, Object> data = containerData.get(0);
            DockerContainer container = new DockerContainer();
            container.setId(UUID.randomUUID().toString());
            container.setContainerId(containerId);
            
            Map<String, Object> config = (Map<String, Object>) data.get("Config");
            if (config != null) {
                container.setName((String) config.get("Hostname"));
                container.setImageName((String) config.get("Image"));
                container.setCommand((String) config.get("Cmd"));
                
                Map<String, String> labels = (Map<String, String>) config.get("Labels");
                container.setLabels(labels);
                
                List<String> envList = (List<String>) config.get("Env");
                if (envList != null) {
                    Map<String, String> envMap = envList.stream()
                            .filter(env -> env.contains("="))
                            .collect(Collectors.toMap(
                                    env -> env.substring(0, env.indexOf('=')),
                                    env -> env.substring(env.indexOf('=') + 1),
                                    (v1, v2) -> v2
                            ));
                    container.setEnv(envMap);
                }
            }
            
            Map<String, Object> state = (Map<String, Object>) data.get("State");
            if (state != null) {
                container.setState((String) state.get("Status"));
                container.setStatus((String) state.get("Status"));
                
                String startedAt = (String) state.get("StartedAt");
                if (startedAt != null && !startedAt.isEmpty() && !startedAt.equals("0001-01-01T00:00:00Z")) {
                    container.setStartedAt(LocalDateTime.parse(startedAt.substring(0, 19)));
                }
                
                String finishedAt = (String) state.get("FinishedAt");
                if (finishedAt != null && !finishedAt.isEmpty() && !finishedAt.equals("0001-01-01T00:00:00Z")) {
                    container.setFinishedAt(LocalDateTime.parse(finishedAt.substring(0, 19)));
                }
                
                container.setExitCode((Integer) state.get("ExitCode"));
                container.setRestartCount((Integer) state.get("RestartCount"));
            }
            
            Map<String, Object> hostConfig = (Map<String, Object>) data.get("HostConfig");
            if (hostConfig != null) {
                container.setHostConfig(hostConfig);
            }
            
            Map<String, Object> networkSettings = (Map<String, Object>) data.get("NetworkSettings");
            if (networkSettings != null) {
                container.setNetworkSettings(networkSettings);
                
                Map<String, Object> ports = (Map<String, Object>) networkSettings.get("Ports");
                if (ports != null) {
                    List<String> portList = new ArrayList<>();
                    for (Map.Entry<String, Object> entry : ports.entrySet()) {
                        portList.add(entry.getKey() + " -> " + entry.getValue());
                    }
                    container.setPorts(portList);
                }
                
                Map<String, Object> networks = (Map<String, Object>) networkSettings.get("Networks");
                if (networks != null) {
                    container.setNetworks(new ArrayList<>(networks.keySet()));
                }
            }
            
            List<Map<String, Object>> mounts = (List<Map<String, Object>>) data.get("Mounts");
            if (mounts != null) {
                container.setMountPoints(mounts);
                
                List<String> volumes = mounts.stream()
                        .map(mount -> mount.get("Source") + ":" + mount.get("Destination"))
                        .collect(Collectors.toList());
                container.setVolumes(volumes);
            }
            
            return container;
        } catch (Exception e) {
            logger.error("Error fetching container {} from context {}: {}", 
                    containerId, context.getName(), e.getMessage());
            return null;
        }
    }

    /**
     * Start a container.
     *
     * @param contextId The context ID
     * @param containerId The container ID
     * @return True if started successfully
     */
    public boolean startContainer(String contextId, String containerId) {
        DockerContext context = contextService.getContextById(contextId);
        if (context == null) {
            return false;
        }
        
        try {
            List<String> command = new ArrayList<>();
            command.add("container");
            command.add("start");
            command.add(containerId);
            
            String output = contextService.executeCommand(contextId, command);
            
            return output.contains(containerId);
        } catch (Exception e) {
            logger.error("Error starting container {} in context {}: {}", 
                    containerId, context.getName(), e.getMessage());
            return false;
        }
    }

    /**
     * Stop a container.
     *
     * @param contextId The context ID
     * @param containerId The container ID
     * @return True if stopped successfully
     */
    public boolean stopContainer(String contextId, String containerId) {
        DockerContext context = contextService.getContextById(contextId);
        if (context == null) {
            return false;
        }
        
        try {
            List<String> command = new ArrayList<>();
            command.add("container");
            command.add("stop");
            command.add(containerId);
            
            String output = contextService.executeCommand(contextId, command);
            
            return output.contains(containerId);
        } catch (Exception e) {
            logger.error("Error stopping container {} in context {}: {}", 
                    containerId, context.getName(), e.getMessage());
            return false;
        }
    }

    /**
     * Restart a container.
     *
     * @param contextId The context ID
     * @param containerId The container ID
     * @return True if restarted successfully
     */
    public boolean restartContainer(String contextId, String containerId) {
        DockerContext context = contextService.getContextById(contextId);
        if (context == null) {
            return false;
        }
        
        try {
            List<String> command = new ArrayList<>();
            command.add("container");
            command.add("restart");
            command.add(containerId);
            
            String output = contextService.executeCommand(contextId, command);
            
            return output.contains(containerId);
        } catch (Exception e) {
            logger.error("Error restarting container {} in context {}: {}", 
                    containerId, context.getName(), e.getMessage());
            return false;
        }
    }

    /**
     * Remove a container.
     *
     * @param contextId The context ID
     * @param containerId The container ID
     * @param force Whether to force removal
     * @return True if removed successfully
     */
    public boolean removeContainer(String contextId, String containerId, boolean force) {
        DockerContext context = contextService.getContextById(contextId);
        if (context == null) {
            return false;
        }
        
        try {
            List<String> command = new ArrayList<>();
            command.add("container");
            command.add("rm");
            
            if (force) {
                command.add("--force");
            }
            
            command.add(containerId);
            
            String output = contextService.executeCommand(contextId, command);
            
            return output.contains(containerId);
        } catch (Exception e) {
            logger.error("Error removing container {} in context {}: {}", 
                    containerId, context.getName(), e.getMessage());
            return false;
        }
    }

    /**
     * Get container logs.
     *
     * @param contextId The context ID
     * @param containerId The container ID
     * @param tail Number of lines to show from the end of the logs
     * @return List of Docker logs
     */
    public List<DockerLog> getContainerLogs(String contextId, String containerId, int tail) {
        DockerContext context = contextService.getContextById(contextId);
        if (context == null) {
            return Collections.emptyList();
        }
        
        try {
            List<String> command = new ArrayList<>();
            command.add("container");
            command.add("logs");
            command.add("--timestamps");
            
            if (tail > 0) {
                command.add("--tail");
                command.add(String.valueOf(tail));
            }
            
            command.add(containerId);
            
            String output = contextService.executeCommand(contextId, command);
            
            List<DockerLog> logs = new ArrayList<>();
            long lineNumber = 1;
            
            for (String line : output.split("\n")) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                
                DockerLog log = new DockerLog();
                log.setId(UUID.randomUUID().toString());
                log.setContainerId(containerId);
                
                // Parse timestamp and message
                if (line.length() > 30) {
                    String timestamp = line.substring(0, 30).trim();
                    String message = line.substring(30).trim();
                    
                    log.setTimestamp(LocalDateTime.parse(timestamp.substring(0, 19)));
                    log.setMessage(message);
                } else {
                    log.setMessage(line);
                }
                
                log.setLineNumber(lineNumber++);
                log.setStream("stdout");  // Default to stdout
                
                logs.add(log);
            }
            
            return logs;
        } catch (Exception e) {
            logger.error("Error fetching logs for container {} in context {}: {}", 
                    containerId, context.getName(), e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Stream container logs.
     *
     * @param contextId The context ID
     * @param containerId The container ID
     * @return True if streaming started successfully
     */
    public boolean streamContainerLogs(String contextId, String containerId) {
        DockerContext context = contextService.getContextById(contextId);
        if (context == null) {
            return false;
        }
        
        // Stop any existing log stream for this container
        stopLogStream(contextId, containerId);
        
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            
            List<String> command = new ArrayList<>();
            command.add("docker");
            command.add("container");
            command.add("logs");
            command.add("--timestamps");
            command.add("--follow");
            command.add(containerId);
            
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
            
            processBuilder.command(command);
            
            Process process = processBuilder.start();
            logStreams.put(contextId + "-" + containerId, process);
            
            // Start a thread to read the log stream
            new Thread(() -> {
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        // Process log line (in a real application, send to WebSocket or SSE)
                        logger.debug("Container {} log: {}", containerId, line);
                    }
                } catch (Exception e) {
                    logger.error("Error reading log stream for container {} in context {}: {}", 
                            containerId, contextId, e.getMessage());
                } finally {
                    logStreams.remove(contextId + "-" + containerId);
                }
            }).start();
            
            return true;
        } catch (Exception e) {
            logger.error("Error streaming logs for container {} in context {}: {}", 
                    containerId, context.getName(), e.getMessage());
            return false;
        }
    }

    /**
     * Stop streaming container logs.
     *
     * @param contextId The context ID
     * @param containerId The container ID
     * @return True if streaming stopped successfully
     */
    public boolean stopLogStream(String contextId, String containerId) {
        Process process = logStreams.get(contextId + "-" + containerId);
        if (process == null) {
            return false;
        }
        
        process.destroy();
        logStreams.remove(contextId + "-" + containerId);
        
        return true;
    }

    /**
     * Get container state from status string.
     *
     * @param status The container status string
     * @return The container state
     */
    private String getContainerState(String status) {
        if (status == null) {
            return "unknown";
        }
        
        status = status.toLowerCase();
        
        if (status.contains("running")) {
            return "running";
        } else if (status.contains("created")) {
            return "created";
        } else if (status.contains("exited")) {
            return "exited";
        } else if (status.contains("paused")) {
            return "paused";
        } else if (status.contains("restarting")) {
            return "restarting";
        } else if (status.contains("removing")) {
            return "removing";
        } else if (status.contains("dead")) {
            return "dead";
        } else {
            return "unknown";
        }
    }
}

