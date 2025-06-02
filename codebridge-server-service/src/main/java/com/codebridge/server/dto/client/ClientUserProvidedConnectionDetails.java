package com.codebridge.server.dto.client;

import com.codebridge.server.model.enums.ServerAuthProvider; // Assuming this enum is appropriate
import java.util.UUID;

// This DTO is sent by ServerService to SessionService
// It contains the actual connection details resolved by ServerService
public class ClientUserProvidedConnectionDetails {
    private String hostname;
    private int port;
    private String username; // The specific remote username to use
    private ServerAuthProvider authProvider;
    private String decryptedPassword; // If password auth
    private String decryptedPrivateKey; // If SSH key auth (actual private key material)
    private String sshKeyName; // Optional: name/identifier for the key, mainly for logging/reference

    // Constructors, Getters, Setters
    public ClientUserProvidedConnectionDetails() {}

    public ClientUserProvidedConnectionDetails(String hostname, int port, String username, ServerAuthProvider authProvider) {
        this.hostname = hostname;
        this.port = port;
        this.username = username;
        this.authProvider = authProvider;
    }
    
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

    public String getDecryptedPassword() {
        return decryptedPassword;
    }

    public void setDecryptedPassword(String decryptedPassword) {
        this.decryptedPassword = decryptedPassword;
    }

    public String getDecryptedPrivateKey() {
        return decryptedPrivateKey;
    }

    public void setDecryptedPrivateKey(String decryptedPrivateKey) {
        this.decryptedPrivateKey = decryptedPrivateKey;
    }

    public String getSshKeyName() {
        return sshKeyName;
    }

    public void setSshKeyName(String sshKeyName) {
        this.sshKeyName = sshKeyName;
    }
}
