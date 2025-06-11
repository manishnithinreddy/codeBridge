package com.jcraft.jsch;

/**
 * Interface for a host key repository.
 * This is a simplified version for test purposes.
 */
public interface HostKeyRepository {
    /**
     * Host key status: OK
     */
    int OK = 0;
    
    /**
     * Host key status: NOT_INCLUDED
     */
    int NOT_INCLUDED = 1;
    
    /**
     * Host key status: CHANGED
     */
    int CHANGED = 2;
    
    /**
     * Check the host key.
     * @param host the host
     * @param key the key
     * @return the check result
     */
    int check(String host, byte[] key);
    
    /**
     * Add the host key.
     * @param hostkey the host key
     * @param ui the user info
     */
    void add(HostKey hostkey, UserInfo ui);
    
    /**
     * Remove the host key.
     * @param host the host
     * @param type the key type
     */
    void remove(String host, String type);
    
    /**
     * Remove the host key.
     * @param host the host
     * @param type the key type
     * @param key the key
     */
    void remove(String host, String type, byte[] key);
    
    /**
     * Get the host key.
     * @param host the host
     * @param type the key type
     * @return the host key
     */
    HostKey[] getHostKey(String host, String type);
    
    /**
     * Get the host key.
     * @return the host key
     */
    HostKey[] getHostKey();
    
    /**
     * Get the host key repository name.
     * @return the name
     */
    String getKnownHostsRepositoryID();
}

