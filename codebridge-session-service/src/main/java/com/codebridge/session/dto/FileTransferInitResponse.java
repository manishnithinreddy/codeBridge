package com.codebridge.session.dto;

import com.codebridge.session.service.transfer.ChunkedFileTransferService.TransferDirection;
import java.io.Serializable;

public class FileTransferInitResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private String transferId;
    private TransferDirection direction;
    private Long totalSize;
    private String fileName;
    private String remotePath;

    // Constructors
    public FileTransferInitResponse() {}

    public FileTransferInitResponse(String transferId, TransferDirection direction, Long totalSize, String fileName, String remotePath) {
        this.transferId = transferId;
        this.direction = direction;
        this.totalSize = totalSize;
        this.fileName = fileName;
        this.remotePath = remotePath;
    }

    // Getters and Setters
    public String getTransferId() {
        return transferId;
    }

    public void setTransferId(String transferId) {
        this.transferId = transferId;
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

    public String getRemotePath() {
        return remotePath;
    }

    public void setRemotePath(String remotePath) {
        this.remotePath = remotePath;
    }
}

