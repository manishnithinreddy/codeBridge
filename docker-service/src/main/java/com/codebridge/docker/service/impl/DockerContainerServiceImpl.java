package com.codebridge.docker.service.impl;

import com.codebridge.docker.model.ContainerInfo;
import com.codebridge.docker.service.DockerContainerService;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DockerContainerServiceImpl implements DockerContainerService {

    private final DockerClient dockerClient;

    public DockerContainerServiceImpl(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    @Override
    public List<ContainerInfo> getContainers(boolean all) {
        log.info("Getting all containers, all={}", all);
        
        try {
            List<Container> containers = dockerClient.listContainersCmd()
                .withShowAll(all)
                .exec();
            
            return containers.stream()
                .map(this::mapToContainerInfo)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting containers", e);
            return new ArrayList<>();
        }
    }

    @Override
    public ContainerInfo getContainer(String containerId) {
        log.info("Getting container: {}", containerId);
        
        try {
            List<Container> containers = dockerClient.listContainersCmd()
                .withShowAll(true)
                .withIdFilter(List.of(containerId))
                .exec();
            
            if (!containers.isEmpty()) {
                return mapToContainerInfo(containers.get(0));
            }
        } catch (Exception e) {
            log.error("Error getting container: {}", containerId, e);
        }
        
        return null;
    }

    @Override
    public String createContainer(String image, String name, Map<String, String> env, 
                                 Map<String, String> ports, Map<String, String> volumes) {
        log.info("Creating container with image: {}, name: {}", image, name);
        
        try {
            CreateContainerCmd createCmd = dockerClient.createContainerCmd(image)
                .withName(name);
            
            // Set environment variables
            if (env != null && !env.isEmpty()) {
                List<String> envList = env.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.toList());
                createCmd.withEnv(envList);
            }
            
            // Set port bindings
            if (ports != null && !ports.isEmpty()) {
                Ports portBindings = new Ports();
                ports.forEach((containerPort, hostPort) -> {
                    ExposedPort exposedPort = ExposedPort.tcp(Integer.parseInt(containerPort));
                    Ports.Binding binding = Ports.Binding.bindPort(Integer.parseInt(hostPort));
                    portBindings.bind(exposedPort, binding);
                });
                createCmd.withHostConfig(HostConfig.newHostConfig().withPortBindings(portBindings));
            }
            
            // Set volumes (simplified for demo)
            // In a real implementation, we would use proper volume bindings
            
            CreateContainerResponse response = createCmd.exec();
            return response.getId();
        } catch (Exception e) {
            log.error("Error creating container", e);
            throw new RuntimeException("Failed to create container: " + e.getMessage());
        }
    }

    @Override
    public boolean startContainer(String containerId) {
        log.info("Starting container: {}", containerId);
        
        try {
            dockerClient.startContainerCmd(containerId).exec();
            return true;
        } catch (Exception e) {
            log.error("Error starting container: {}", containerId, e);
            return false;
        }
    }

    @Override
    public boolean stopContainer(String containerId) {
        log.info("Stopping container: {}", containerId);
        
        try {
            dockerClient.stopContainerCmd(containerId).exec();
            return true;
        } catch (Exception e) {
            log.error("Error stopping container: {}", containerId, e);
            return false;
        }
    }

    @Override
    public boolean removeContainer(String containerId, boolean force, boolean removeVolumes) {
        log.info("Removing container: {}, force: {}, removeVolumes: {}", containerId, force, removeVolumes);
        
        try {
            dockerClient.removeContainerCmd(containerId)
                .withForce(force)
                .withRemoveVolumes(removeVolumes)
                .exec();
            return true;
        } catch (Exception e) {
            log.error("Error removing container: {}", containerId, e);
            return false;
        }
    }

    @Override
    public String getContainerLogs(String containerId, Integer tail, boolean timestamps, 
                                  String since, String until) {
        log.info("Getting logs for container: {}", containerId);
        
        try {
            LogContainerCmd logCmd = dockerClient.logContainerCmd(containerId)
                .withStdOut(true)
                .withStdErr(true)
                .withTimestamps(timestamps);
            
            if (tail != null) {
                logCmd.withTail(tail);
            }
            
            if (since != null && !since.isEmpty()) {
                logCmd.withSince((int) (Instant.parse(since).getEpochSecond()));
            }
            
            if (until != null && !until.isEmpty()) {
                logCmd.withUntil((int) (Instant.parse(until).getEpochSecond()));
            }
            
            StringBuilder logStringBuilder = new StringBuilder();
            LogContainerResultCallback callback = new LogContainerResultCallback() {
                @Override
                public void onNext(com.github.dockerjava.api.model.frame.Frame frame) {
                    logStringBuilder.append(new String(frame.getPayload()));
                    logStringBuilder.append(System.lineSeparator());
                    super.onNext(frame);
                }
            };
            
            logCmd.exec(callback).awaitCompletion(30, TimeUnit.SECONDS);
            return logStringBuilder.toString();
        } catch (Exception e) {
            log.error("Error getting logs for container: {}", containerId, e);
            return "Error retrieving logs: " + e.getMessage();
        }
    }
    
    private ContainerInfo mapToContainerInfo(Container container) {
        Map<String, String> labels = container.getLabels() != null ? container.getLabels() : new HashMap<>();
        
        return ContainerInfo.builder()
            .id(container.getId())
            .name(container.getNames()[0].startsWith("/") ? 
                 container.getNames()[0].substring(1) : container.getNames()[0])
            .image(container.getImage())
            .imageId(container.getImageId())
            .command(container.getCommand())
            .created(ZonedDateTime.ofInstant(
                Instant.ofEpochSecond(container.getCreated()), 
                ZoneId.systemDefault()))
            .state(container.getState())
            .status(container.getStatus())
            .labels(labels)
            .build();
    }
}

