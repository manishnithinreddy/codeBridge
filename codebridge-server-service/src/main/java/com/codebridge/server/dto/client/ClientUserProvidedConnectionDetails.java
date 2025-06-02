package com.codebridge.server.dto.client;

// Mirrors UserProvidedConnectionDetails in SessionService
public class ClientUserProvidedConnectionDetails {
    private String hostname;
    private int port;
    private String username;
    private byte[] decryptedPrivateKey;
    private byte[] publicKey;

    public ClientUserProvidedConnectionDetails(String hostname, int port, String username, byte[] decryptedPrivateKey, byte[] publicKey) {
        this.hostname = hostname;
        this.port = port;
        this.username = username;
        this.decryptedPrivateKey = decryptedPrivateKey;
        this.publicKey = publicKey;
    }

    // Getters - Setters might not be needed if only used for sending
    public String getHostname() { return hostname; }
    public int getPort() { return port; }
    public String getUsername() { return username; }
    public byte[] getDecryptedPrivateKey() { return decryptedPrivateKey; }
    public byte[] getPublicKey() { return publicKey; }

    public void setHostname(String hostname) { this.hostname = hostname; }
    public void setPort(int port) { this.port = port; }
    public void setUsername(String username) { this.username = username; }
    public void setDecryptedPrivateKey(byte[] decryptedPrivateKey) { this.decryptedPrivateKey = decryptedPrivateKey; }
    public void setPublicKey(byte[] publicKey) { this.publicKey = publicKey; }
}
