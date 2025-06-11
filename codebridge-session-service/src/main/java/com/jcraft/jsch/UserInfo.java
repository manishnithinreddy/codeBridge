package com.jcraft.jsch;

/**
 * Interface for user information.
 * This is a simplified version for test purposes.
 */
public interface UserInfo {
    /**
     * Get the passphrase.
     * @return the passphrase
     */
    String getPassphrase();
    
    /**
     * Get the password.
     * @return the password
     */
    String getPassword();
    
    /**
     * Prompt for the passphrase.
     * @param message the prompt message
     * @return true if the passphrase was provided
     */
    boolean promptPassphrase(String message);
    
    /**
     * Prompt for the password.
     * @param message the prompt message
     * @return true if the password was provided
     */
    boolean promptPassword(String message);
    
    /**
     * Prompt for yes/no.
     * @param message the prompt message
     * @return true if yes was selected
     */
    boolean promptYesNo(String message);
    
    /**
     * Show a message.
     * @param message the message
     */
    void showMessage(String message);
}

