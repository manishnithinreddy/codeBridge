package com.codebridge.docker.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Model class for Docker container information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContainerInfo {
    
    private String id;
    private String name;
    private String image;
    private String imageId;
    private String command;
    private LocalDateTime created;
    private String state;
    private String status;
    private Map<String, String> ports;
    private Map<String, String> labels;
    private Map<String, Object> networkSettings;
    private String networkMode;
    private Map<String, Object> mounts;
}

