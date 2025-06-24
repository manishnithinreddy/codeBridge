package com.codebridge.session.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for chunk metadata
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkMetadata {
    
    /**
     * Chunk number (0-based)
     */
    private Integer chunkNumber;
    
    /**
     * Size of the chunk in bytes
     */
    private Long size;
    
    /**
     * Whether the chunk has been uploaded
     */
    private Boolean uploaded;
    
    /**
     * Offset in the file where this chunk starts
     */
    private Long offset;
    
    /**
     * MD5 checksum of the chunk (optional)
     */
    private String checksum;
}

