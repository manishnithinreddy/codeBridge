# CodeBridge Feature Flag Client (Java)

Java client library for the CodeBridge Feature Flag Service.

## Overview

This client library provides a simple interface for interacting with the Feature Flag Service, enabling dynamic service implementation switching and feature toggling in Java applications.

## Features

- **Flag Retrieval**: Get flag values by key
- **Flag Evaluation**: Evaluate flags with context
- **Real-time Updates**: Stream flag changes in real-time
- **Caching**: Local caching for high-performance flag retrieval
- **Type-safe Values**: Type-safe flag value retrieval

## Installation

Add the dependency to your Maven project:

```xml
<dependency>
    <groupId>com.codebridge</groupId>
    <artifactId>feature-flag-client-java</artifactId>
    <version>1.0.0</version>
</dependency>
```

Or to your Gradle project:

```groovy
implementation 'com.codebridge:feature-flag-client-java:1.0.0'
```

## Usage

### Creating a Client

```java
// Create a client with default namespace
FeatureFlagClient client = new FeatureFlagClient("localhost", 9090);

// Create a client with custom namespace
FeatureFlagClient client = new FeatureFlagClient("localhost", 9090, "production");
```

### Getting a Flag

```java
// Get a flag by key
FlagValue value = client.getFlag("service-implementation");

// Get a flag by key and namespace
FlagValue value = client.getFlag("service-implementation", "production");

// Check flag type and get value
if (value.getType() == FlagValueType.STRING) {
    String implementation = value.getStringValue();
    // Use the implementation
}
```

### Evaluating a Flag with Context

```java
// Create evaluation context
EvaluationContext context = EvaluationContext.builder()
    .userId("user-123")
    .sessionId("session-456")
    .attribute("environment", "production")
    .numericAttribute("concurrent_connections", 5000)
    .booleanAttribute("is_premium", true)
    .serviceContext(ServiceContext.builder()
        .serviceName("session-service")
        .serviceVersion("1.0.0")
        .environment("production")
        .instanceId("instance-789")
        .metric("cpu_usage", "0.75")
        .build())
    .build();

// Evaluate flag with context
EvaluationResult result = client.evaluateFlag("session-service.implementation", context);

// Use the result
if (result.getValue().getType() == FlagValueType.STRING) {
    String implementation = result.getValue().getStringValue();
    if (implementation.equals("go")) {
        // Use Go implementation
    } else {
        // Use Java implementation
    }
}

// Check evaluation reason
if (result.getReason().getType() == EvaluationResult.ReasonType.RULE_MATCH) {
    System.out.println("Rule matched: " + result.getReason().getRuleId());
}
```

### Streaming Flag Updates

```java
// Stream flag updates for specific flags
List<String> flagKeys = Arrays.asList("service-implementation", "feature-enabled");
client.streamFlagUpdates(flagKeys, "production", event -> {
    System.out.println("Flag updated: " + event.getFlagKey());
    
    // Update local state based on the event
    if (event.getFlagKey().equals("service-implementation")) {
        if (event.hasStringValue()) {
            String implementation = event.getStringValue();
            // Update implementation
        }
    }
});

// Stream all flag updates in a namespace
client.streamFlagUpdates(Collections.emptyList(), "production", event -> {
    System.out.println("Flag updated: " + event.getFlagKey());
    // Handle update
});
```

### Setting a Flag

```java
// Set a boolean flag
client.setFlag("feature-enabled", FlagValue.ofBoolean(true));

// Set a string flag with namespace, description, and tags
Map<String, String> tags = new HashMap<>();
tags.put("category", "service-routing");
tags.put("owner", "platform-team");

client.setFlag(
    "service-implementation",
    "production",
    FlagValue.ofString("go"),
    "Controls which implementation of the service to use",
    tags
);
```

### Closing the Client

```java
// Close the client when done
client.close();

// Or use try-with-resources
try (FeatureFlagClient client = new FeatureFlagClient("localhost", 9090)) {
    // Use the client
}
```

## Best Practices

1. **Reuse Client Instances**: Create a single client instance and reuse it throughout your application.
2. **Handle Exceptions**: Wrap client calls in try-catch blocks to handle network errors.
3. **Provide Default Values**: Always have a fallback value in case the flag service is unavailable.
4. **Use Streaming for Real-time Updates**: Use the streaming API for real-time updates instead of polling.
5. **Include Relevant Context**: Provide as much context as possible when evaluating flags for accurate targeting.

## Example: Service Implementation Switching

```java
public class ServiceFactory {
    private final FeatureFlagClient flagClient;
    
    public ServiceFactory(FeatureFlagClient flagClient) {
        this.flagClient = flagClient;
    }
    
    public SessionService createSessionService(String userId, String environment) {
        // Create context
        EvaluationContext context = EvaluationContext.builder()
            .userId(userId)
            .attribute("environment", environment)
            .serviceContext(ServiceContext.builder()
                .serviceName("session-service")
                .environment(environment)
                .build())
            .build();
        
        // Evaluate flag
        EvaluationResult result = flagClient.evaluateFlag("session-service.implementation", context);
        
        // Create service based on flag value
        if (result.getValue() != null && result.getValue().getType() == FlagValueType.STRING) {
            String implementation = result.getValue().getStringValue();
            
            switch (implementation) {
                case "go":
                    return new GoSessionService();
                case "java":
                    return new JavaSessionService();
                default:
                    return new JavaSessionService(); // Default implementation
            }
        }
        
        // Default to Java implementation if flag evaluation fails
        return new JavaSessionService();
    }
}
```

