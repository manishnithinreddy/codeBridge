package com.jcraft.jsch;

/**
 * Class for a host key.
 * This is a simplified version for test purposes.
 */
public class HostKey {
    /**
     * Host key type: GUESS
     */
    public static final int GUESS = 0;
    
    private String host;
    private int port;
    private int type;
    private byte[] key;
    private String keyType;
    
    /**
     * Constructor for a host key.
     * @param host the host
     * @param type the key type
     * @param key the key
     * @throws JSchException if the key is invalid
     */
    public HostKey(String host, int type, byte[] key) throws JSchException {
        this.host = host;
        this.type = type;
        this.key = key;
        this.keyType = "ssh-rsa"; // Default for testing
    }
    
    /**
     * Constructor for a host key with port.
     * @param host the host
     * @param port the port
     * @param type the key type
     * @param key the key
     * @throws JSchException if the key is invalid
     */
    public HostKey(String host, int port, int type, byte[] key) throws JSchException {
        this.host = host;
        this.port = port;
        this.type = type;
        this.key = key;
        this.keyType = "ssh-rsa"; // Default for testing
    }
    
    /**
     * Get the host.
     * @return the host
     */
    public String getHost() {
        return host;
    }
    
    /**
     * Get the key type.
     * @return the key type
     */
    public String getType() {
        return keyType;
    }
    
    /**
     * Get the key as a Base64 string.
     * @return the key
     */
    public String getKey() {
        return java.util.Base64.getEncoder().encodeToString(key);
    }
}

