package com.codebridge.session.model;

import java.io.Serializable;
import java.util.UUID;

// Using a record for conciseness and automatic hashCode/equals/toString
public record SessionKey(
    UUID platformUserId,
    UUID resourceId, // e.g., Server ID for SSH, could be DB alias ID for DB
    String sessionType // e.g., "SSH", "DB_POSTGRESQL"
) implements Serializable {
    // No explicit constructor, getters, hashCode, equals, or toString needed for records
}
