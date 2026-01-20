package com.codebridge.docker.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageInfo {
    private String id;
    private List<String> repoTags;
    private List<String> repoDigests;
    private ZonedDateTime created;
    private Long size;
    private Long virtualSize;
    private Map<String, String> labels;
    private Map<String, Object> containerConfig;
}

