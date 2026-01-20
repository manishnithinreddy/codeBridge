package com.codebridge.documentation.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO representing a search result.
 */
@Data
@NoArgsConstructor
public class SearchResult {

    private UUID id;
    private SearchIndexType type;
    private String title;
    private String description;
    private String path;
    private String method;
    private String serviceName;
    private String versionName;
}

