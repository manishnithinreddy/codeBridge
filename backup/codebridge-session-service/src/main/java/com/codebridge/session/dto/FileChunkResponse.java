package com.codebridge.session.dto;

import java.io.Serializable;

public class FileChunkResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private String transferId;
    private Integer chunkIndex;
    private Long currentOffset;
    private Long totalSize;
    private boolean complete;
    private byte[] data; // For downloads

    // Constructors
    public FileChunkResponse() {}

    public FileChunkResponse(String transferId, Integer chunkIndex, Long currentOffset, Long totalSize, boolean complete) {
        this.transferId = transferId;
        this.chunkIndex = chunkIndex;
        this.currentOffset = currentOffset;
        this.totalSize = totalSize;
        this.complete = complete;
    }

    public FileChunkResponse(String transferId, Integer chunkIndex, Long currentOffset, Long totalSize, boolean complete, byte[] data) {
        this.transferId = transferId;
        this.chunkIndex = chunkIndex;
        this.currentOffset = currentOffset;
        this.totalSize = totalSize;
        this.complete = complete;
        this.data = data;
    }

    // Getters and Setters
    public String getTransferId() {
        return transferId;
    }

    public void setTransferId(String transferId) {
        this.transferId = transferId;
    }

    public Integer getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(Integer chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    public Long getCurrentOffset() {
        return currentOffset;
    }

    public void setCurrentOffset(Long currentOffset) {
        this.currentOffset = currentOffset;
    }

    public Long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(Long totalSize) {
        this.totalSize = totalSize;
    }

    public boolean isComplete() {
        return complete;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}

