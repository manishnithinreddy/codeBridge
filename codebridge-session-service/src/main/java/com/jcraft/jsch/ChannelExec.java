package com.jcraft.jsch;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Class for a JSch exec channel.
 * This is a simplified version for test purposes.
 */
public class ChannelExec extends Channel {
    private String command;
    private InputStream errStream;
    
    /**
     * Constructor for a channel.
     */
    public ChannelExec() {
        super();
    }
    
    /**
     * Set the command to execute.
     * @param command the command
     */
    public void setCommand(String command) {
        this.command = command;
    }
    
    /**
     * Get the command.
     * @return the command
     */
    public String getCommand() {
        return this.command;
    }
    
    /**
     * Set the output stream.
     * @param out the output stream
     */
    public void setOutputStream(OutputStream out) {
        // Empty method for testing
    }
    
    /**
     * Set the output stream.
     * @param out the output stream
     * @param dontClose whether to close the stream
     */
    public void setOutputStream(OutputStream out, boolean dontClose) {
        // Empty method for testing
    }
    
    /**
     * Set the error stream.
     * @param err the error stream
     */
    public void setErrStream(OutputStream err) {
        // Empty method for testing
    }
    
    /**
     * Set the error stream.
     * @param err the error stream
     * @param dontClose whether to close the stream
     */
    public void setErrStream(OutputStream err, boolean dontClose) {
        // Empty method for testing
    }
    
    /**
     * Get the error stream.
     * @return the error stream
     * @throws JSchException if there's an error getting the error stream
     */
    public InputStream getErrStream() throws JSchException {
        return this.errStream;
    }
    
    /**
     * Check if the channel is closed.
     * @return true if the channel is closed
     */
    public boolean isClosed() {
        return !isConnected();
    }
}

