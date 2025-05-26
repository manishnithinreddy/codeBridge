package com.codebridge.docker.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * Request DTO for container creation.
 */
public class ContainerCreationRequest {

    @NotBlank(message = "Name cannot be blank")
    private String name;

    @NotBlank(message = "Image cannot be blank")
    private String image;

    private Map<String, String> ports;

    private Map<String, String> environment;

    @NotNull(message = "Memory limit cannot be null")
    @Min(value = 1, message = "Memory limit must be at least 1 MB")
    private Long memoryLimit;

    @NotNull(message = "CPU limit cannot be null")
    @Min(value = 1, message = "CPU limit must be at least 1")
    private Long cpuLimit;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Map<String, String> getPorts() {
        return ports;
    }

    public void setPorts(Map<String, String> ports) {
        this.ports = ports;
    }

    public Map<String, String> getEnvironment() {
        return environment;
    }

    public void setEnvironment(Map<String, String> environment) {
        this.environment = environment;
    }

    public Long getMemoryLimit() {
        return memoryLimit;
    }

    public void setMemoryLimit(Long memoryLimit) {
        this.memoryLimit = memoryLimit;
    }

    public Long getCpuLimit() {
        return cpuLimit;
    }

    public void setCpuLimit(Long cpuLimit) {
        this.cpuLimit = cpuLimit;
    }
}

