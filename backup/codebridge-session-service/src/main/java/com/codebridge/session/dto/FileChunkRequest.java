package com.codebridge.session.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

public class FileChunkRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull
    private Integer chunkIndex;

    @NotNull
    @Min(0)
    private Long offset;

    @NotNull
    @Min(1)
    private Integer chunkSize;

    private byte[] data; // For uploads

    // Constructors
    public FileChunkRequest() {}

    public FileChunkRequest(Integer chunkIndex, Long offset, Integer chunkSize, byte[] data) {
        this.chunkIndex = chunkIndex;
        this.offset = offset;
        this.chunkSize = chunkSize;
        this.data = data;
    }

    // Getters and Setters
    public Integer getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(Integer chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    public Long getOffset() {
        return offset;
    }

    public void setOffset(Long offset) {
        this.offset = offset;
    }

    public Integer getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(Integer chunkSize) {
        this.chunkSize = chunkSize;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}

