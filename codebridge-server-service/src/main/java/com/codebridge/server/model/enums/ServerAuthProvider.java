package com.codebridge.server.model.enums;

public enum ServerAuthProvider {
    SSH_KEY,
    PASSWORD
    // Future: NONE (for unprotected servers, though risky), AGENT_FORWARDING
}
