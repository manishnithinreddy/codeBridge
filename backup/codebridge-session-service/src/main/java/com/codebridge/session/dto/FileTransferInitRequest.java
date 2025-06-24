package com.codebridge.session.dto;

import com.codebridge.session.service.transfer.ChunkedFileTransferService.TransferDirection;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

public class FileTransferInitRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank
    private String remotePath;

    @NotNull
    private TransferDirection direction;

    @NotNull
    @Min(0)
    private Long totalSize;

    @NotBlank
    private String fileName;

    // Constructors
    public FileTransferInitRequest() {}

    public FileTransferInitRequest(String remotePath, TransferDirection direction, Long totalSize, String fileName) {
        this.remotePath = remotePath;
        this.direction = direction;
        this.totalSize = totalSize;
        this.fileName = fileName;
    }

    // Getters and Setters
    public String getRemotePath() {
        return remotePath;
    }

    public void setRemotePath(String remotePath) {
        this.remotePath = remotePath;
    }

    public TransferDirection getDirection() {
        return direction;
    }

    public void setDirection(TransferDirection direction) {
        this.direction = direction;
    }

    public Long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(Long totalSize) {
        this.totalSize = totalSize;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}

