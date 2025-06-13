package com.codebridge.aidb.model;

import java.io.Serializable;
import java.util.UUID;

// Using a record for conciseness and automatic hashCode/equals/toString
public record SessionKey(
    UUID platformUserId,
    UUID resourceId, // e.g., DB alias ID for DB
    String sessionType // e.g., "DB_POSTGRESQL"
) implements Serializable {
    // No explicit constructor, getters, hashCode, equals, or toString needed for records
}

