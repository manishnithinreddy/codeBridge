package com.codebridge.docker.service.impl;

import com.codebridge.docker.model.ContainerInfo;
import com.codebridge.docker.service.DockerContainerService;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Implementation of DockerContainerService using Docker Java client.
 */
@Slf4j
@Service
public class DockerContainerServiceImpl implements DockerContainerService {

    private final DockerClient dockerClient;

    public DockerContainerServiceImpl(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    @Override
    public List<ContainerInfo> getContainers(boolean showAll) {
        log.info("Getting all containers, showAll: {}", showAll);
        
        try {
            List<Container> containers = dockerClient.listContainersCmd()
                    .withShowAll(showAll)
                    .exec();
            
            return containers.stream()
                    .map(this::mapToContainerInfo)
                    .collect(Collectors.toList());
        } catch (DockerException e) {
            log.error("Error getting containers", e);
            return Collections.emptyList();
        }
    }

    @Override
    public ContainerInfo getContainer(String containerIdOrName) {
        log.info("Getting container: {}", containerIdOrName);
        
        try {
            InspectContainerResponse containerResponse = dockerClient.inspectContainerCmd(containerIdOrName)
                    .exec();
            
            return mapToContainerInfo(containerResponse);
        } catch (DockerException e) {
            log.error("Error getting container: {}", containerIdOrName, e);
            return null;
        }
    }

    @Override
    public ContainerInfo createContainer(String image, String name, Map<String, String> env, 
                                        Map<String, String> ports, Map<String, String> volumes, 
                                        String[] cmd) {
        log.info("Creating container with image: {}, name: {}", image, name);
        
        try {
            CreateContainerCmd containerCmd = dockerClient.createContainerCmd(image)
                    .withName(name);
            
            // Set environment variables
            if (env != null && !env.isEmpty()) {
                List<String> envList = env.entrySet().stream()
                        .map(entry -> entry.getKey() + "=" + entry.getValue())
                        .collect(Collectors.toList());
                containerCmd.withEnv(envList);
            }
            
            // Set port bindings
            if (ports != null && !ports.isEmpty()) {
                Map<ExposedPort, Ports.Binding[]> portBindings = new HashMap<>();
                for (Map.Entry<String, String> entry : ports.entrySet()) {
                    String[] containerPortParts = entry.getKey().split("/");
                    int containerPort = Integer.parseInt(containerPortParts[0]);
                    String protocol = containerPortParts.length > 1 ? containerPortParts[1] : "tcp";
                    
                    ExposedPort exposedPort = ExposedPort.tcp(containerPort);
                    if ("udp".equalsIgnoreCase(protocol)) {
                        exposedPort = ExposedPort.udp(containerPort);
                    }
                    
                    Ports.Binding binding = Ports.Binding.bindPort(Integer.parseInt(entry.getValue()));
                    portBindings.put(exposedPort, new Ports.Binding[]{binding});
                }
                containerCmd.withPortBindings(new PortBinding(portBindings));
            }
            
            // Set volume bindings
            if (volumes != null && !volumes.isEmpty()) {
                List<Bind> binds = volumes.entrySet().stream()
                        .map(entry -> new Bind(entry.getKey(), new Volume(entry.getValue())))
                        .collect(Collectors.toList());
                containerCmd.withBinds(binds);
            }
            
            // Set command
            if (cmd != null && cmd.length > 0) {
                containerCmd.withCmd(cmd);
            }
            
            CreateContainerResponse response = containerCmd.exec();
            
            return getContainer(response.getId());
        } catch (DockerException e) {
            log.error("Error creating container with image: {}, name: {}", image, name, e);
            return null;
        }
    }

    @Override
    public boolean startContainer(String containerIdOrName) {
        log.info("Starting container: {}", containerIdOrName);
        
        try {
            dockerClient.startContainerCmd(containerIdOrName).exec();
            return true;
        } catch (DockerException e) {
            log.error("Error starting container: {}", containerIdOrName, e);
            return false;
        }
    }

    @Override
    public boolean stopContainer(String containerIdOrName, int timeout) {
        log.info("Stopping container: {} with timeout: {}", containerIdOrName, timeout);
        
        try {
            dockerClient.stopContainerCmd(containerIdOrName)
                    .withTimeout(timeout)
                    .exec();
            return true;
        } catch (DockerException e) {
            log.error("Error stopping container: {}", containerIdOrName, e);
            return false;
        }
    }

    @Override
    public boolean restartContainer(String containerIdOrName, int timeout) {
        log.info("Restarting container: {} with timeout: {}", containerIdOrName, timeout);
        
        try {
            dockerClient.restartContainerCmd(containerIdOrName)
                    .withtTimeout(timeout)
                    .exec();
            return true;
        } catch (DockerException e) {
            log.error("Error restarting container: {}", containerIdOrName, e);
            return false;
        }
    }

    @Override
    public boolean pauseContainer(String containerIdOrName) {
        log.info("Pausing container: {}", containerIdOrName);
        
        try {
            dockerClient.pauseContainerCmd(containerIdOrName).exec();
            return true;
        } catch (DockerException e) {
            log.error("Error pausing container: {}", containerIdOrName, e);
            return false;
        }
    }

    @Override
    public boolean unpauseContainer(String containerIdOrName) {
        log.info("Unpausing container: {}", containerIdOrName);
        
        try {
            dockerClient.unpauseContainerCmd(containerIdOrName).exec();
            return true;
        } catch (DockerException e) {
            log.error("Error unpausing container: {}", containerIdOrName, e);
            return false;
        }
    }

    @Override
    public boolean killContainer(String containerIdOrName) {
        log.info("Killing container: {}", containerIdOrName);
        
        try {
            dockerClient.killContainerCmd(containerIdOrName).exec();
            return true;
        } catch (DockerException e) {
            log.error("Error killing container: {}", containerIdOrName, e);
            return false;
        }
    }

    @Override
    public boolean removeContainer(String containerIdOrName, boolean removeVolumes, boolean force) {
        log.info("Removing container: {}, removeVolumes: {}, force: {}", containerIdOrName, removeVolumes, force);
        
        try {
            dockerClient.removeContainerCmd(containerIdOrName)
                    .withRemoveVolumes(removeVolumes)
                    .withForce(force)
                    .exec();
            return true;
        } catch (DockerException e) {
            log.error("Error removing container: {}", containerIdOrName, e);
            return false;
        }
    }

    @Override
    public String getContainerLogs(String containerIdOrName, int tail, boolean timestamps) {
        log.info("Getting logs for container: {}, tail: {}, timestamps: {}", containerIdOrName, tail, timestamps);
        
        try {
            LogContainerCmd logCmd = dockerClient.logContainerCmd(containerIdOrName)
                    .withStdOut(true)
                    .withStdErr(true)
                    .withTimestamps(timestamps);
            
            if (tail > 0) {
                logCmd.withTail(tail);
            }
            
            final StringBuilder logString = new StringBuilder();
            logCmd.exec(new LogContainerResultCallback() {
                @Override
                public void onNext(Frame frame) {
                    logString.append(new String(frame.getPayload()));
                    logString.append(System.lineSeparator());
                }
            }).awaitCompletion(30, TimeUnit.SECONDS);
            
            return logString.toString();
        } catch (DockerException | InterruptedException e) {
            log.error("Error getting logs for container: {}", containerIdOrName, e);
            return "Error retrieving container logs: " + e.getMessage();
        }
    }

    @Override
    public Map<String, Object> getContainerStats(String containerIdOrName) {
        log.info("Getting stats for container: {}", containerIdOrName);
        
        try {
            // In a real implementation, this would use dockerClient.statsCmd()
            // For now, we'll just return a placeholder
            Map<String, Object> stats = new HashMap<>();
            stats.put("cpu_usage", 0.5);
            stats.put("memory_usage", 100 * 1024 * 1024);
            stats.put("memory_limit", 1024 * 1024 * 1024);
            stats.put("network_rx", 1024 * 1024);
            stats.put("network_tx", 512 * 1024);
            
            return stats;
        } catch (DockerException e) {
            log.error("Error getting stats for container: {}", containerIdOrName, e);
            return Collections.emptyMap();
        }
    }
    
    /**
     * Maps Docker Container to ContainerInfo.
     *
     * @param container Docker Container
     * @return ContainerInfo
     */
    private ContainerInfo mapToContainerInfo(Container container) {
        ContainerInfo info = new ContainerInfo();
        info.setId(container.getId());
        info.setName(container.getNames()[0].startsWith("/") ? 
                container.getNames()[0].substring(1) : container.getNames()[0]);
        info.setImage(container.getImage());
        info.setImageId(container.getImageId());
        info.setCommand(container.getCommand());
        info.setCreated(LocalDateTime.ofInstant(
                Instant.ofEpochSecond(container.getCreated()), 
                ZoneId.systemDefault()));
        info.setState(container.getState());
        info.setStatus(container.getStatus());
        
        // Map ports
        Map<String, String> ports = new HashMap<>();
        if (container.getPorts() != null) {
            for (Port port : container.getPorts()) {
                if (port.getPublicPort() != null) {
                    ports.put(port.getPrivatePort() + "/" + port.getType(), 
                            String.valueOf(port.getPublicPort()));
                }
            }
        }
        info.setPorts(ports);
        
        // Map labels
        info.setLabels(container.getLabels());
        
        return info;
    }
    
    /**
     * Maps InspectContainerResponse to ContainerInfo.
     *
     * @param container InspectContainerResponse
     * @return ContainerInfo
     */
    private ContainerInfo mapToContainerInfo(InspectContainerResponse container) {
        ContainerInfo info = new ContainerInfo();
        info.setId(container.getId());
        info.setName(container.getName().startsWith("/") ? 
                container.getName().substring(1) : container.getName());
        info.setImage(container.getImageId());
        info.setImageId(container.getImageId());
        info.setCommand(Arrays.toString(container.getConfig().getCmd()));
        info.setCreated(LocalDateTime.ofInstant(
                Instant.ofEpochSecond(container.getCreated().getTime() / 1000), 
                ZoneId.systemDefault()));
        info.setState(container.getState().getStatus());
        info.setStatus(container.getState().getStatus());
        
        // Map network settings
        Map<String, Object> networkSettings = new HashMap<>();
        networkSettings.put("ipAddress", container.getNetworkSettings().getIpAddress());
        networkSettings.put("gateway", container.getNetworkSettings().getGateway());
        networkSettings.put("macAddress", container.getNetworkSettings().getMacAddress());
        info.setNetworkSettings(networkSettings);
        
        // Map network mode
        info.setNetworkMode(container.getHostConfig().getNetworkMode());
        
        // Map labels
        info.setLabels(container.getConfig().getLabels());
        
        // Map mounts
        Map<String, String> mounts = new HashMap<>();
        if (container.getMounts() != null) {
            for (InspectContainerResponse.Mount mount : container.getMounts()) {
                mounts.put(mount.getSource(), mount.getDestination());
            }
        }
        info.setMounts(mounts);
        
        return info;
    }
}

