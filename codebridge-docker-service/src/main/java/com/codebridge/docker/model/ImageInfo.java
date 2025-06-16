package com.codebridge.docker.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Represents Docker image information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImageInfo {
    
    private String id;
    private String parentId;
    private List<String> repoTags;
    private List<String> repoDigests;
    private LocalDateTime created;
    private Long size;
    private Long virtualSize;
    private Map<String, String> labels;
    private Map<String, String> containerConfig;
}

