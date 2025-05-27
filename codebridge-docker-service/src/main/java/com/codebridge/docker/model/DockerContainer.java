package com.codebridge.docker.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Model class representing a Docker Container.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DockerContainer {

    private String id;
    
    @JsonProperty("container_id")
    private String containerId;
    
    private String name;
    
    @JsonProperty("image_id")
    private String imageId;
    
    @JsonProperty("image_name")
    private String imageName;
    
    private String status;
    
    private String state;
    
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    
    @JsonProperty("started_at")
    private LocalDateTime startedAt;
    
    @JsonProperty("finished_at")
    private LocalDateTime finishedAt;
    
    private List<String> ports;
    
    private List<String> volumes;
    
    private List<String> networks;
    
    private Map<String, String> labels;
    
    private Map<String, String> env;
    
    @JsonProperty("host_config")
    private Map<String, Object> hostConfig;
    
    @JsonProperty("network_settings")
    private Map<String, Object> networkSettings;
    
    @JsonProperty("mount_points")
    private List<Map<String, Object>> mountPoints;
    
    @JsonProperty("resource_usage")
    private Map<String, Object> resourceUsage;
    
    @JsonProperty("health_status")
    private String healthStatus;
    
    @JsonProperty("restart_count")
    private Integer restartCount;
    
    @JsonProperty("exit_code")
    private Integer exitCode;
    
    @JsonProperty("command")
    private String command;

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContainerId() {
        return containerId;
    }

    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public List<String> getPorts() {
        return ports;
    }

    public void setPorts(List<String> ports) {
        this.ports = ports;
    }

    public List<String> getVolumes() {
        return volumes;
    }

    public void setVolumes(List<String> volumes) {
        this.volumes = volumes;
    }

    public List<String> getNetworks() {
        return networks;
    }

    public void setNetworks(List<String> networks) {
        this.networks = networks;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }

    public Map<String, String> getEnv() {
        return env;
    }

    public void setEnv(Map<String, String> env) {
        this.env = env;
    }

    public Map<String, Object> getHostConfig() {
        return hostConfig;
    }

    public void setHostConfig(Map<String, Object> hostConfig) {
        this.hostConfig = hostConfig;
    }

    public Map<String, Object> getNetworkSettings() {
        return networkSettings;
    }

    public void setNetworkSettings(Map<String, Object> networkSettings) {
        this.networkSettings = networkSettings;
    }

    public List<Map<String, Object>> getMountPoints() {
        return mountPoints;
    }

    public void setMountPoints(List<Map<String, Object>> mountPoints) {
        this.mountPoints = mountPoints;
    }

    public Map<String, Object> getResourceUsage() {
        return resourceUsage;
    }

    public void setResourceUsage(Map<String, Object> resourceUsage) {
        this.resourceUsage = resourceUsage;
    }

    public String getHealthStatus() {
        return healthStatus;
    }

    public void setHealthStatus(String healthStatus) {
        this.healthStatus = healthStatus;
    }

    public Integer getRestartCount() {
        return restartCount;
    }

    public void setRestartCount(Integer restartCount) {
        this.restartCount = restartCount;
    }

    public Integer getExitCode() {
        return exitCode;
    }

    public void setExitCode(Integer exitCode) {
        this.exitCode = exitCode;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }
}

