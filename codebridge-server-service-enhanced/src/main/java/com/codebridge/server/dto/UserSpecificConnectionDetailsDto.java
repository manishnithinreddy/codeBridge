package com.codebridge.server.dto;

import com.codebridge.server.model.SshKey; // SshKey entity
import com.codebridge.server.model.enums.ServerAuthProvider;

public class UserSpecificConnectionDetailsDto {
    private String hostname;
    private int port;
    private String username; // The specific remote username for this user on the server
    private ServerAuthProvider authProvider;
    private SshKey decryptedSshKey; // Nullable - contains decrypted private key if SSH auth
    private String decryptedPassword; // Nullable - contains decrypted password if PWD auth (not used in this phase via ServerUser)

    // Constructor
    public UserSpecificConnectionDetailsDto(String hostname, int port, String username, ServerAuthProvider authProvider) {
        this.hostname = hostname;
        this.port = port;
        this.username = username;
        this.authProvider = authProvider;
    }

    // Getters and Setters
    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public ServerAuthProvider getAuthProvider() {
        return authProvider;
    }

    public void setAuthProvider(ServerAuthProvider authProvider) {
        this.authProvider = authProvider;
    }

    public SshKey getDecryptedSshKey() {
        return decryptedSshKey;
    }

    public void setDecryptedSshKey(SshKey decryptedSshKey) {
        this.decryptedSshKey = decryptedSshKey;
    }

    public String getDecryptedPassword() {
        return decryptedPassword;
    }

    public void setDecryptedPassword(String decryptedPassword) {
        this.decryptedPassword = decryptedPassword;
    }
}
