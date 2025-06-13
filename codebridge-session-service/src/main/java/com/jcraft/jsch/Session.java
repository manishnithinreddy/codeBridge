package com.jcraft.jsch;

import java.util.Properties;

/**
 * Class for a JSch session.
 * This is a simplified version for test purposes.
 */
public class Session {
    private boolean connected = false;
    private Properties config = new Properties();
    
    /**
     * Constructor for a session.
     */
    public Session() {
        // Empty constructor
    }
    
    /**
     * Connect to the remote host.
     * @throws JSchException if there's an error connecting
     */
    public void connect() throws JSchException {
        this.connected = true;
    }
    
    /**
     * Connect to the remote host with a timeout.
     * @param timeout the timeout in milliseconds
     * @throws JSchException if there's an error connecting
     */
    public void connect(int timeout) throws JSchException {
        this.connected = true;
    }
    
    /**
     * Disconnect from the remote host.
     */
    public void disconnect() {
        this.connected = false;
    }
    
    /**
     * Check if the session is connected.
     * @return true if the session is connected
     */
    public boolean isConnected() {
        return this.connected;
    }
    
    /**
     * Set the password for authentication.
     * @param password the password
     */
    public void setPassword(String password) {
        // Empty method for testing
    }
    
    /**
     * Set the configuration properties.
     * @param config the configuration properties
     */
    public void setConfig(Properties config) {
        this.config = config;
    }
    
    /**
     * Open a channel.
     * @param type the channel type
     * @return the channel
     * @throws JSchException if there's an error opening the channel
     */
    public Channel openChannel(String type) throws JSchException {
        if ("exec".equals(type)) {
            return new ChannelExec();
        }
        return new Channel();
    }
}

