package com.codebridge.aidb.config;

import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

@Component
public class ApplicationInstanceIdProvider {
    private final String instanceId;

    public ApplicationInstanceIdProvider() {
        // Generate a unique ID for this service instance
        // In production, might use k8s pod ID, EC2 instance ID, etc.
        String hostname;
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            hostname = "unknown-host";
        }
        this.instanceId = hostname + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    public String getInstanceId() {
        return instanceId;
    }
}

