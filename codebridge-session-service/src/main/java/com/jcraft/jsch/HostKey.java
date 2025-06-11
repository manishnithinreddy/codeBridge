package com.jcraft.jsch;

/**
 * Class for a JSch host key.
 * This is a simplified version for test purposes.
 */
public class HostKey {
    private String host;
    private int port;
    private String type;
    private byte[] key;
    private String comment;
    
    /**
     * Host key type: GUESS
     */
    public static final int GUESS = 0;
    
    /**
     * Host key type: SSHRSA
     */
    public static final int SSHRSA = 1;
    
    /**
     * Host key type: SSHDSS
     */
    public static final int SSHDSS = 2;
    
    /**
     * Host key type: ECDSA256
     */
    public static final int ECDSA256 = 3;
    
    /**
     * Host key type: ECDSA384
     */
    public static final int ECDSA384 = 4;
    
    /**
     * Host key type: ECDSA521
     */
    public static final int ECDSA521 = 5;
    
    /**
     * Host key type: ED25519
     */
    public static final int ED25519 = 6;
    
    /**
     * Constructor for a host key.
     * @param host the host
     * @param type the key type
     * @param key the key
     * @throws JSchException if there's an error creating the host key
     */
    public HostKey(String host, int type, byte[] key) throws JSchException {
        this(host, 22, type, key);
    }
    
    /**
     * Constructor for a host key.
     * @param host the host
     * @param port the port
     * @param type the key type
     * @param key the key
     * @throws JSchException if there's an error creating the host key
     */
    public HostKey(String host, int port, int type, byte[] key) throws JSchException {
        this.host = host;
        this.port = port;
        this.key = key;
        
        // Determine the key type
        switch (type) {
            case SSHRSA:
                this.type = "ssh-rsa";
                break;
            case SSHDSS:
                this.type = "ssh-dss";
                break;
            case ECDSA256:
                this.type = "ecdsa-sha2-nistp256";
                break;
            case ECDSA384:
                this.type = "ecdsa-sha2-nistp384";
                break;
            case ECDSA521:
                this.type = "ecdsa-sha2-nistp521";
                break;
            case ED25519:
                this.type = "ssh-ed25519";
                break;
            case GUESS:
            default:
                // For GUESS, we would normally determine the type from the key bytes
                // For simplicity, we'll default to ssh-rsa
                this.type = "ssh-rsa";
                break;
        }
    }
    
    /**
     * Get the host.
     * @return the host
     */
    public String getHost() {
        return this.host;
    }
    
    /**
     * Get the key type.
     * @return the key type
     */
    public String getType() {
        return this.type;
    }
    
    /**
     * Get the key.
     * @return the key
     */
    public String getKey() {
        // In a real implementation, this would return the Base64-encoded key
        return "AAAAB3NzaC1yc2EAAAADAQABAAABAQC0g+ZTxC7weoIJLUafOgrm+h";
    }
    
    /**
     * Get the finger print.
     * @return the finger print
     * @throws JSchException if there's an error getting the finger print
     */
    public String getFingerPrint(JSch jsch) throws JSchException {
        // In a real implementation, this would calculate the fingerprint
        return "MD5:00:11:22:33:44:55:66:77:88:99:aa:bb:cc:dd:ee:ff";
    }
}

