package com.codebridge.session.service;

import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class ApplicationInstanceIdProvider {

    private final String instanceId;

    public ApplicationInstanceIdProvider() {
        // Generate a unique ID for this application instance at startup.
        // A more sophisticated approach might involve environment variables or discovery service metadata.
        this.instanceId = UUID.randomUUID().toString();
    }

    public String getInstanceId() {
        return instanceId;
    }
}
