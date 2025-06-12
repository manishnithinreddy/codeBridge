package com.codebridge.server.dto.file;

public class RemoteFileEntry {
    private String filename;
    private boolean isDirectory;
    private long size;
    private String lastModified; // Can be epoch long or formatted string from SftpATTRS
    private String permissions;  // e.g., "drwxr-xr-x"

    public RemoteFileEntry() {
    }

    public RemoteFileEntry(String filename, boolean isDirectory, long size, String lastModified, String permissions) {
        this.filename = filename;
        this.isDirectory = isDirectory;
        this.size = size;
        this.lastModified = lastModified;
        this.permissions = permissions;
    }

    // Getters and Setters
    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public void setDirectory(boolean directory) {
        isDirectory = directory;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getLastModified() {
        return lastModified;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }

    public String getPermissions() {
        return permissions;
    }

    public void setPermissions(String permissions) {
        this.permissions = permissions;
    }
}
