package com.codebridge.scalability.model;

/**
 * Enumeration of supported session store types.
 */
public enum SessionStoreType {
    /**
     * Redis-based session store.
     */
    REDIS,
    
    /**
     * Hazelcast-based session store.
     */
    HAZELCAST,
    
    /**
     * JDBC-based session store.
     */
    JDBC
}

