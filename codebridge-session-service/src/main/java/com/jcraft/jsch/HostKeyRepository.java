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
}

