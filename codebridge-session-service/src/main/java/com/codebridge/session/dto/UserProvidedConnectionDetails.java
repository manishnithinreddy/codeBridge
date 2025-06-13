package com.codebridge.session.dto;

import com.codebridge.session.model.enums.ServerAuthProvider; // Assuming this enum will be created in session.model.enums
import java.io.Serializable;

public class UserProvidedConnectionDetails implements Serializable {
    private static final long serialVersionUID = 1L;

    private String hostname;
    private int port;
    private String username;
    private ServerAuthProvider authProvider;
    private String decryptedPassword;
    private String decryptedPrivateKey;
    private String sshKeyName;

    // Constructors
    public UserProvidedConnectionDetails() {}

    public UserProvidedConnectionDetails(String hostname, int port, String username, ServerAuthProvider authProvider) {
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
