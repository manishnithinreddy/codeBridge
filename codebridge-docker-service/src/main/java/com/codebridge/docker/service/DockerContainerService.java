package com.codebridge.docker.service;

import com.codebridge.docker.model.ContainerInfo;

import java.util.List;
import java.util.Map;

/**
 * Service for Docker container operations.
 */
public interface DockerContainerService {
    
    /**
     * Gets all containers.
     *
     * @param showAll Whether to show all containers (including stopped ones)
     * @return List of container information
     */
    List<ContainerInfo> getContainers(boolean showAll);
    
    /**
     * Gets a specific container by ID or name.
     *
     * @param containerIdOrName ID or name of the container
     * @return Container information or null if not found
     */
    ContainerInfo getContainer(String containerIdOrName);
    
    /**
     * Creates a new container.
     *
     * @param image Image to use for the container
     * @param name Name for the container
     * @param env Environment variables
     * @param ports Port mappings
     * @param volumes Volume mappings
     * @param cmd Command to run
     * @return Created container information
     */
    ContainerInfo createContainer(String image, String name, Map<String, String> env, 
                                 Map<String, String> ports, Map<String, String> volumes, 
                                 String[] cmd);
    
    /**
     * Starts a container.
     *
     * @param containerIdOrName ID or name of the container
     * @return true if successful, false otherwise
     */
    boolean startContainer(String containerIdOrName);
    
    /**
     * Stops a container.
     *
     * @param containerIdOrName ID or name of the container
     * @param timeout Timeout in seconds before killing the container
     * @return true if successful, false otherwise
     */
    boolean stopContainer(String containerIdOrName, int timeout);
    
    /**
     * Restarts a container.
     *
     * @param containerIdOrName ID or name of the container
     * @param timeout Timeout in seconds before killing the container
     * @return true if successful, false otherwise
     */
    boolean restartContainer(String containerIdOrName, int timeout);
    
    /**
     * Pauses a container.
     *
     * @param containerIdOrName ID or name of the container
     * @return true if successful, false otherwise
     */
    boolean pauseContainer(String containerIdOrName);
    
    /**
     * Unpauses a container.
     *
     * @param containerIdOrName ID or name of the container
     * @return true if successful, false otherwise
     */
    boolean unpauseContainer(String containerIdOrName);
    
    /**
     * Kills a container.
     *
     * @param containerIdOrName ID or name of the container
     * @return true if successful, false otherwise
     */
    boolean killContainer(String containerIdOrName);
    
    /**
     * Removes a container.
     *
     * @param containerIdOrName ID or name of the container
     * @param removeVolumes Whether to remove volumes
     * @param force Whether to force removal
     * @return true if successful, false otherwise
     */
    boolean removeContainer(String containerIdOrName, boolean removeVolumes, boolean force);
    
    /**
     * Gets logs for a container.
     *
     * @param containerIdOrName ID or name of the container
     * @param tail Number of lines to show from the end of the logs
     * @param timestamps Whether to show timestamps
     * @return Container logs as a string
     */
    String getContainerLogs(String containerIdOrName, int tail, boolean timestamps);
    
    /**
     * Gets stats for a container.
     *
     * @param containerIdOrName ID or name of the container
     * @return Container stats as a map
     */
    Map<String, Object> getContainerStats(String containerIdOrName);
}

