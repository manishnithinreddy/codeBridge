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
     * Check a host key.
     * @param host the host
     * @param key the key
     * @return the status
     */
    int check(String host, byte[] key);
    
    /**
     * Add a host key.
     * @param hostkey the host key
     * @param ui the user info
     */
    void add(HostKey hostkey, UserInfo ui);
    
    /**
     * Remove a host key.
     * @param host the host
     * @param type the key type
     */
    void remove(String host, String type);
    
    /**
     * Remove a host key.
     * @param host the host
     * @param type the key type
     * @param key the key
     */
    void remove(String host, String type, byte[] key);
    
    /**
     * Get all host keys.
     * @return the host keys
     */
    HostKey[] getHostKey();
    
    /**
     * Get host keys for a specific host and type.
     * @param host the host
     * @param type the key type
     * @return the host keys
     */
    HostKey[] getHostKey(String host, String type);
    
    /**
     * Get the repository ID.
     * @return the repository ID
     */
    String getKnownHostsRepositoryID();
}

