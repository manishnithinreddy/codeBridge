package com.codebridge.docker.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContainerInfo {
    private String id;
    private String name;
    private String image;
    private String imageId;
    private String command;
    private ZonedDateTime created;
    private String state;
    private String status;
    private Map<String, String> labels;
    private Map<String, Object> hostConfig;
    private Map<String, Object> networkSettings;
    private Map<String, Object> mounts;
}

