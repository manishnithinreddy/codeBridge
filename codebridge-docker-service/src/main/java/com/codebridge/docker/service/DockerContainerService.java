package com.codebridge.docker.service;

import com.codebridge.docker.dto.ContainerCreationRequest;
import com.codebridge.docker.dto.ContainerResponse;
import com.codebridge.docker.exception.ContainerOperationException;
import com.codebridge.docker.exception.ResourceNotFoundException;
import com.codebridge.docker.model.Container;
import com.codebridge.docker.model.ContainerStatus;
import com.codebridge.docker.repository.ContainerRepository;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for Docker container operations.
 */
@Service
public class DockerContainerService {

    private final DockerClient dockerClient;
    private final ContainerRepository containerRepository;

    public DockerContainerService(DockerClient dockerClient, ContainerRepository containerRepository) {
        this.dockerClient = dockerClient;
        this.containerRepository = containerRepository;
    }

    /**
     * Creates a new Docker container.
     *
     * @param request the container creation request
     * @param userId the user ID
     * @param teamId the team ID
     * @return the created container
     */
    @Transactional
    public ContainerResponse createContainer(ContainerCreationRequest request, UUID userId, UUID teamId) {
        try {
            // Create port bindings
            List<PortBinding> portBindings = new ArrayList<>();
            if (request.getPorts() != null) {
                portBindings = request.getPorts().entrySet().stream()
                        .map(entry -> PortBinding.parse(entry.getKey() + ":" + entry.getValue()))
                        .collect(Collectors.toList());
            }
            
            // Create host config
            HostConfig hostConfig = HostConfig.newHostConfig()
                    .withPortBindings(portBindings)
                    .withMemory(request.getMemoryLimit())
                    .withCpuCount(request.getCpuLimit());
            
            // Create container
            CreateContainerResponse createResponse = dockerClient.createContainerCmd(request.getImage())
                    .withName(request.getName())
                    .withHostConfig(hostConfig)
                    .withEnv(mapToEnvList(request.getEnvironment()))
                    .exec();
            
            // Start container
            dockerClient.startContainerCmd(createResponse.getId()).exec();
            
            // Inspect container
            InspectContainerResponse inspectResponse = dockerClient.inspectContainerCmd(createResponse.getId()).exec();
            
            // Save container to database
            Container container = new Container();
            container.setId(UUID.randomUUID());
            container.setName(request.getName());
            container.setImage(request.getImage());
            container.setContainerId(createResponse.getId());
            container.setUserId(userId);
            container.setTeamId(teamId);
            container.setStatus(ContainerStatus.RUNNING);
            container.setPorts(request.getPorts().toString());
            container.setEnvironment(request.getEnvironment().toString());
            container.setMemoryLimit(request.getMemoryLimit());
            container.setCpuLimit(request.getCpuLimit());
            
            container = containerRepository.save(container);
            
            return mapToContainerResponse(container, inspectResponse);
        } catch (DockerException e) {
            throw new ContainerOperationException("Failed to create container: " + e.getMessage(), e);
        }
    }

    /**
     * Gets all containers for a user.
     *
     * @param userId the user ID
     * @return the list of containers
     */
    public List<ContainerResponse> getContainers(UUID userId) {
        List<Container> containers = containerRepository.findByUserId(userId);
        
        return containers.stream()
                .map(container -> {
                    try {
                        InspectContainerResponse inspectResponse = dockerClient.inspectContainerCmd(container.getContainerId()).exec();
                        return mapToContainerResponse(container, inspectResponse);
                    } catch (DockerException e) {
                        // Container might have been removed outside of our system
                        container.setStatus(ContainerStatus.UNKNOWN);
                        containerRepository.save(container);
                        return mapToContainerResponse(container, null);
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * Gets a container by ID.
     *
     * @param id the container ID
     * @param userId the user ID
     * @return the container
     */
    public ContainerResponse getContainer(UUID id, UUID userId) {
        Container container = containerRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Container", "id", id));
        
        try {
            InspectContainerResponse inspectResponse = dockerClient.inspectContainerCmd(container.getContainerId()).exec();
            return mapToContainerResponse(container, inspectResponse);
        } catch (DockerException e) {
            // Container might have been removed outside of our system
            container.setStatus(ContainerStatus.UNKNOWN);
            containerRepository.save(container);
            return mapToContainerResponse(container, null);
        }
    }

    /**
     * Starts a container.
     *
     * @param id the container ID
     * @param userId the user ID
     * @return the updated container
     */
    @Transactional
    public ContainerResponse startContainer(UUID id, UUID userId) {
        Container container = containerRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Container", "id", id));
        
        try {
            dockerClient.startContainerCmd(container.getContainerId()).exec();
            
            container.setStatus(ContainerStatus.RUNNING);
            container = containerRepository.save(container);
            
            InspectContainerResponse inspectResponse = dockerClient.inspectContainerCmd(container.getContainerId()).exec();
            return mapToContainerResponse(container, inspectResponse);
        } catch (DockerException e) {
            throw new ContainerOperationException("Failed to start container: " + e.getMessage(), e);
        }
    }

    /**
     * Stops a container.
     *
     * @param id the container ID
     * @param userId the user ID
     * @return the updated container
     */
    @Transactional
    public ContainerResponse stopContainer(UUID id, UUID userId) {
        Container container = containerRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Container", "id", id));
        
        try {
            dockerClient.stopContainerCmd(container.getContainerId()).exec();
            
            container.setStatus(ContainerStatus.STOPPED);
            container = containerRepository.save(container);
            
            InspectContainerResponse inspectResponse = dockerClient.inspectContainerCmd(container.getContainerId()).exec();
            return mapToContainerResponse(container, inspectResponse);
        } catch (DockerException e) {
            throw new ContainerOperationException("Failed to stop container: " + e.getMessage(), e);
        }
    }

    /**
     * Removes a container.
     *
     * @param id the container ID
     * @param userId the user ID
     */
    @Transactional
    public void removeContainer(UUID id, UUID userId) {
        Container container = containerRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Container", "id", id));
        
        try {
            // Stop container if running
            if (container.getStatus() == ContainerStatus.RUNNING) {
                dockerClient.stopContainerCmd(container.getContainerId()).exec();
            }
            
            // Remove container
            dockerClient.removeContainerCmd(container.getContainerId()).exec();
            
            // Remove from database
            containerRepository.delete(container);
        } catch (DockerException e) {
            throw new ContainerOperationException("Failed to remove container: " + e.getMessage(), e);
        }
    }

    /**
     * Maps a container and inspect response to a container response.
     *
     * @param container the container
     * @param inspectResponse the inspect response
     * @return the container response
     */
    private ContainerResponse mapToContainerResponse(Container container, InspectContainerResponse inspectResponse) {
        ContainerResponse response = new ContainerResponse();
        response.setId(container.getId());
        response.setName(container.getName());
        response.setImage(container.getImage());
        response.setContainerId(container.getContainerId());
        response.setStatus(container.getStatus());
        response.setPorts(container.getPorts());
        response.setEnvironment(container.getEnvironment());
        response.setMemoryLimit(container.getMemoryLimit());
        response.setCpuLimit(container.getCpuLimit());
        response.setCreatedAt(container.getCreatedAt());
        
        if (inspectResponse != null) {
            response.setIpAddress(inspectResponse.getNetworkSettings().getIpAddress());
            response.setRunning(inspectResponse.getState().getRunning());
            response.setStartedAt(inspectResponse.getState().getStartedAt());
        }
        
        return response;
    }

    /**
     * Maps environment variables to a list of strings.
     *
     * @param environment the environment variables
     * @return the list of environment variables
     */
    private List<String> mapToEnvList(Map<String, String> environment) {
        if (environment == null) {
            return new ArrayList<>();
        }
        
        return environment.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.toList());
    }
}

