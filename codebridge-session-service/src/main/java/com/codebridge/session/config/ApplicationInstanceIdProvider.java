package com.codebridge.session.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class ApplicationInstanceIdProvider {

    private final String instanceId;

    // Prefer injecting a unique ID, e.g., from Spring Cloud's Eureka instance ID
    // or generate one if not available (less ideal for true instance tracking across restarts)
    public ApplicationInstanceIdProvider(@Value("${eureka.instance.instance-id:${spring.application.name}:${random.uuid}}") String instanceId) {
        if (instanceId == null || instanceId.contains("${random.uuid}")) {
            // If instanceId is not set by discovery service or is the default with random.uuid placeholder
            this.instanceId = UUID.randomUUID().toString();
        } else {
            this.instanceId = instanceId;
        }
    }

    public StringgetInstanceId() {
        return instanceId;
    }
}
