package com.codebridge.docker.service;

import com.codebridge.docker.model.ContainerInfo;

import java.util.List;
import java.util.Map;

public interface DockerContainerService {
    
    /**
     * Get a list of all containers
     * 
     * @param all If true, show all containers (default shows just running)
     * @return List of container information
     */
    List<ContainerInfo> getContainers(boolean all);
    
    /**
     * Get detailed information about a specific container
     * 
     * @param containerId ID or name of the container
     * @return Container details
     */
    ContainerInfo getContainer(String containerId);
    
    /**
     * Create a new container
     * 
     * @param image Image to use for the container
     * @param name Name for the container
     * @param env Environment variables
     * @param ports Port mappings
     * @param volumes Volume mappings
     * @return ID of the created container
     */
    String createContainer(String image, String name, Map<String, String> env, 
                          Map<String, String> ports, Map<String, String> volumes);
    
    /**
     * Start a container
     * 
     * @param containerId ID or name of the container
     * @return true if successful
     */
    boolean startContainer(String containerId);
    
    /**
     * Stop a container
     * 
     * @param containerId ID or name of the container
     * @return true if successful
     */
    boolean stopContainer(String containerId);
    
    /**
     * Remove a container
     * 
     * @param containerId ID or name of the container
     * @param force Force removal of running container
     * @param removeVolumes Remove associated volumes
     * @return true if successful
     */
    boolean removeContainer(String containerId, boolean force, boolean removeVolumes);
    
    /**
     * Get logs from a container
     * 
     * @param containerId ID or name of the container
     * @param tail Number of lines to show from the end of the logs
     * @param timestamps Include timestamps
     * @param since Show logs since timestamp
     * @param until Show logs until timestamp
     * @return Container logs as a string
     */
    String getContainerLogs(String containerId, Integer tail, boolean timestamps, 
                           String since, String until);
}

