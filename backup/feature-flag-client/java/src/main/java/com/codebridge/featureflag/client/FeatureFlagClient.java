package com.codebridge.featureflag.client;

import com.codebridge.featureflag.grpc.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Client for the Feature Flag Service.
 */
public class FeatureFlagClient implements AutoCloseable {
    
    private static final Logger logger = LoggerFactory.getLogger(FeatureFlagClient.class);
    
    private final ManagedChannel channel;
    private final FeatureFlagServiceGrpc.FeatureFlagServiceBlockingStub blockingStub;
    private final FeatureFlagServiceGrpc.FeatureFlagServiceStub asyncStub;
    private final Map<String, FlagValue> flagCache = new ConcurrentHashMap<>();
    private final String defaultNamespace;
    
    /**
     * Creates a new feature flag client.
     * 
     * @param host the host of the feature flag service
     * @param port the port of the feature flag service
     */
    public FeatureFlagClient(String host, int port) {
        this(host, port, "default");
    }
    
    /**
     * Creates a new feature flag client.
     * 
     * @param host the host of the feature flag service
     * @param port the port of the feature flag service
     * @param defaultNamespace the default namespace to use
     */
    public FeatureFlagClient(String host, int port, String defaultNamespace) {
        this.channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        this.blockingStub = FeatureFlagServiceGrpc.newBlockingStub(channel);
        this.asyncStub = FeatureFlagServiceGrpc.newStub(channel);
        this.defaultNamespace = defaultNamespace;
    }
    
    /**
     * Gets a flag by key.
     * 
     * @param key the flag key
     * @return the flag value, or null if not found
     */
    public FlagValue getFlag(String key) {
        return getFlag(key, defaultNamespace);
    }
    
    /**
     * Gets a flag by key and namespace.
     * 
     * @param key the flag key
     * @param namespace the namespace
     * @return the flag value, or null if not found
     */
    public FlagValue getFlag(String key, String namespace) {
        try {
            // Check cache first
            String cacheKey = namespace + ":" + key;
            if (flagCache.containsKey(cacheKey)) {
                return flagCache.get(cacheKey);
            }
            
            // Get from service
            FlagRequest request = FlagRequest.newBuilder()
                    .setFlagKey(key)
                    .setNamespace(namespace)
                    .build();
            
            FlagResponse response = blockingStub.getFlag(request);
            
            if (!response.getExists()) {
                return null;
            }
            
            // Convert to FlagValue
            FlagValue value = convertToFlagValue(response);
            
            // Cache the value
            flagCache.put(cacheKey, value);
            
            return value;
        } catch (StatusRuntimeException e) {
            logger.error("Error getting flag {}: {}", key, e.getStatus());
            if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
                return null;
            }
            throw e;
        }
    }
    
    /**
     * Evaluates a flag with context.
     * 
     * @param key the flag key
     * @param context the evaluation context
     * @return the evaluation result
     */
    public EvaluationResult evaluateFlag(String key, EvaluationContext context) {
        return evaluateFlag(key, defaultNamespace, context);
    }
    
    /**
     * Evaluates a flag with context.
     * 
     * @param key the flag key
     * @param namespace the namespace
     * @param context the evaluation context
     * @return the evaluation result
     */
    public EvaluationResult evaluateFlag(String key, String namespace, EvaluationContext context) {
        try {
            // Convert context to gRPC
            com.codebridge.featureflag.grpc.EvaluationContext grpcContext = convertToGrpcContext(context);
            
            // Create request
            FlagEvaluationRequest request = FlagEvaluationRequest.newBuilder()
                    .setFlagKey(key)
                    .setNamespace(namespace)
                    .setContext(grpcContext)
                    .build();
            
            // Evaluate flag
            FlagEvaluationResponse response = blockingStub.evaluateFlag(request);
            
            // Convert to EvaluationResult
            return convertToEvaluationResult(response);
        } catch (StatusRuntimeException e) {
            logger.error("Error evaluating flag {}: {}", key, e.getStatus());
            throw e;
        }
    }
    
    /**
     * Sets a flag.
     * 
     * @param key the flag key
     * @param value the flag value
     * @return true if the flag was set successfully
     */
    public boolean setFlag(String key, FlagValue value) {
        return setFlag(key, defaultNamespace, value, null, null);
    }
    
    /**
     * Sets a flag.
     * 
     * @param key the flag key
     * @param namespace the namespace
     * @param value the flag value
     * @param description the flag description
     * @param tags the flag tags
     * @return true if the flag was set successfully
     */
    public boolean setFlag(String key, String namespace, FlagValue value, String description, Map<String, String> tags) {
        try {
            // Create request
            SetFlagRequest.Builder requestBuilder = SetFlagRequest.newBuilder()
                    .setFlagKey(key)
                    .setNamespace(namespace);
            
            // Set value based on type
            if (value != null) {
                switch (value.getType()) {
                    case BOOLEAN:
                        requestBuilder.setBoolValue(value.getBooleanValue());
                        break;
                    case STRING:
                        requestBuilder.setStringValue(value.getStringValue());
                        break;
                    case INTEGER:
                        requestBuilder.setIntValue(value.getIntegerValue());
                        break;
                    case DOUBLE:
                        requestBuilder.setDoubleValue(value.getDoubleValue());
                        break;
                    case JSON:
                        requestBuilder.setJsonValue(value.getJsonValue());
                        break;
                }
            }
            
            // Set description if provided
            if (description != null) {
                requestBuilder.setDescription(description);
            }
            
            // Set tags if provided
            if (tags != null) {
                requestBuilder.putAllTags(tags);
            }
            
            // Set flag
            SetFlagResponse response = blockingStub.setFlag(requestBuilder.build());
            
            // Clear cache
            String cacheKey = namespace + ":" + key;
            flagCache.remove(cacheKey);
            
            return response.getSuccess();
        } catch (StatusRuntimeException e) {
            logger.error("Error setting flag {}: {}", key, e.getStatus());
            throw e;
        }
    }
    
    /**
     * Streams flag updates.
     * 
     * @param keys the flag keys to stream
     * @param namespace the namespace
     * @param callback the callback to invoke when a flag is updated
     * @return the stream observer
     */
    public StreamObserver<FlagUpdateEvent> streamFlagUpdates(List<String> keys, String namespace, Consumer<FlagUpdateEvent> callback) {
        // Create request
        FlagStreamRequest.Builder requestBuilder = FlagStreamRequest.newBuilder()
                .setNamespace(namespace);
        
        // Add keys if provided
        if (keys != null) {
            requestBuilder.addAllFlagKeys(keys);
        }
        
        // Create response observer
        StreamObserver<FlagUpdateEvent> responseObserver = new StreamObserver<FlagUpdateEvent>() {
            @Override
            public void onNext(FlagUpdateEvent event) {
                // Update cache
                String cacheKey = event.getNamespace() + ":" + event.getFlagKey();
                
                if (event.getEventType() == EventType.DELETED) {
                    flagCache.remove(cacheKey);
                } else {
                    // Convert to FlagValue
                    FlagValue value = convertToFlagValue(event);
                    flagCache.put(cacheKey, value);
                }
                
                // Invoke callback
                callback.accept(event);
            }
            
            @Override
            public void onError(Throwable t) {
                logger.error("Error streaming flag updates: {}", t.getMessage());
            }
            
            @Override
            public void onCompleted() {
                logger.info("Flag update stream completed");
            }
        };
        
        // Start streaming
        asyncStub.streamFlagUpdates(requestBuilder.build(), responseObserver);
        
        return responseObserver;
    }
    
    /**
     * Converts a gRPC flag response to a flag value.
     * 
     * @param response the gRPC flag response
     * @return the flag value
     */
    private FlagValue convertToFlagValue(FlagResponse response) {
        if (response.hasBoolValue()) {
            return new FlagValue(FlagValueType.BOOLEAN, response.getBoolValue());
        } else if (response.hasStringValue()) {
            return new FlagValue(FlagValueType.STRING, response.getStringValue());
        } else if (response.hasIntValue()) {
            return new FlagValue(FlagValueType.INTEGER, response.getIntValue());
        } else if (response.hasDoubleValue()) {
            return new FlagValue(FlagValueType.DOUBLE, response.getDoubleValue());
        } else if (response.hasJsonValue()) {
            return new FlagValue(FlagValueType.JSON, response.getJsonValue());
        } else {
            return null;
        }
    }
    
    /**
     * Converts a gRPC flag update event to a flag value.
     * 
     * @param event the gRPC flag update event
     * @return the flag value
     */
    private FlagValue convertToFlagValue(FlagUpdateEvent event) {
        if (event.hasBoolValue()) {
            return new FlagValue(FlagValueType.BOOLEAN, event.getBoolValue());
        } else if (event.hasStringValue()) {
            return new FlagValue(FlagValueType.STRING, event.getStringValue());
        } else if (event.hasIntValue()) {
            return new FlagValue(FlagValueType.INTEGER, event.getIntValue());
        } else if (event.hasDoubleValue()) {
            return new FlagValue(FlagValueType.DOUBLE, event.getDoubleValue());
        } else if (event.hasJsonValue()) {
            return new FlagValue(FlagValueType.JSON, event.getJsonValue());
        } else {
            return null;
        }
    }
    
    /**
     * Converts an evaluation context to a gRPC evaluation context.
     * 
     * @param context the evaluation context
     * @return the gRPC evaluation context
     */
    private com.codebridge.featureflag.grpc.EvaluationContext convertToGrpcContext(EvaluationContext context) {
        com.codebridge.featureflag.grpc.EvaluationContext.Builder builder = com.codebridge.featureflag.grpc.EvaluationContext.newBuilder();
        
        if (context.getUserId() != null) {
            builder.setUserId(context.getUserId());
        }
        
        if (context.getSessionId() != null) {
            builder.setSessionId(context.getSessionId());
        }
        
        if (context.getAttributes() != null) {
            builder.putAllAttributes(context.getAttributes());
        }
        
        if (context.getNumericAttributes() != null) {
            builder.putAllNumericAttributes(context.getNumericAttributes());
        }
        
        if (context.getBooleanAttributes() != null) {
            builder.putAllBooleanAttributes(context.getBooleanAttributes());
        }
        
        if (context.getServiceContext() != null) {
            ServiceContext serviceContext = context.getServiceContext();
            ServiceContext.Builder serviceBuilder = ServiceContext.newBuilder();
            
            if (serviceContext.getServiceName() != null) {
                serviceBuilder.setServiceName(serviceContext.getServiceName());
            }
            
            if (serviceContext.getServiceVersion() != null) {
                serviceBuilder.setServiceVersion(serviceContext.getServiceVersion());
            }
            
            if (serviceContext.getInstanceId() != null) {
                serviceBuilder.setInstanceId(serviceContext.getInstanceId());
            }
            
            if (serviceContext.getEnvironment() != null) {
                serviceBuilder.setEnvironment(serviceContext.getEnvironment());
            }
            
            if (serviceContext.getMetrics() != null) {
                serviceBuilder.putAllMetrics(serviceContext.getMetrics());
            }
            
            builder.setServiceContext(serviceBuilder.build());
        }
        
        return builder.build();
    }
    
    /**
     * Converts a gRPC evaluation response to an evaluation result.
     * 
     * @param response the gRPC evaluation response
     * @return the evaluation result
     */
    private EvaluationResult convertToEvaluationResult(FlagEvaluationResponse response) {
        EvaluationResult.Builder builder = EvaluationResult.builder()
                .flagKey(response.getFlagKey());
        
        // Set value based on type
        if (response.hasBoolValue()) {
            builder.value(new FlagValue(FlagValueType.BOOLEAN, response.getBoolValue()));
        } else if (response.hasStringValue()) {
            builder.value(new FlagValue(FlagValueType.STRING, response.getStringValue()));
        } else if (response.hasIntValue()) {
            builder.value(new FlagValue(FlagValueType.INTEGER, response.getIntValue()));
        } else if (response.hasDoubleValue()) {
            builder.value(new FlagValue(FlagValueType.DOUBLE, response.getDoubleValue()));
        } else if (response.hasJsonValue()) {
            builder.value(new FlagValue(FlagValueType.JSON, response.getJsonValue()));
        }
        
        // Set variation ID if available
        if (!response.getVariationId().isEmpty()) {
            builder.variationId(response.getVariationId());
        }
        
        // Set rule ID if available
        if (!response.getRuleId().isEmpty()) {
            builder.ruleId(response.getRuleId());
        }
        
        // Set reason if available
        if (response.hasReason()) {
            EvaluationReason reason = response.getReason();
            EvaluationResult.EvaluationReason.Builder reasonBuilder = EvaluationResult.EvaluationReason.builder();
            
            // Set reason type
            switch (reason.getType()) {
                case DEFAULT:
                    reasonBuilder.type(EvaluationResult.ReasonType.DEFAULT);
                    break;
                case RULE_MATCH:
                    reasonBuilder.type(EvaluationResult.ReasonType.RULE_MATCH);
                    break;
                case PREREQUISITE_FAILED:
                    reasonBuilder.type(EvaluationResult.ReasonType.PREREQUISITE_FAILED);
                    break;
                case ERROR:
                    reasonBuilder.type(EvaluationResult.ReasonType.ERROR);
                    break;
                case DISABLED:
                    reasonBuilder.type(EvaluationResult.ReasonType.DISABLED);
                    break;
                case FALLTHROUGH:
                    reasonBuilder.type(EvaluationResult.ReasonType.FALLTHROUGH);
                    break;
                case TARGET_MATCH:
                    reasonBuilder.type(EvaluationResult.ReasonType.TARGET_MATCH);
                    break;
            }
            
            // Set rule ID if available
            if (!reason.getRuleId().isEmpty()) {
                reasonBuilder.ruleId(reason.getRuleId());
            }
            
            // Set description if available
            if (!reason.getDescription().isEmpty()) {
                reasonBuilder.description(reason.getDescription());
            }
            
            // Set error message if available
            if (!reason.getErrorMessage().isEmpty()) {
                reasonBuilder.errorMessage(reason.getErrorMessage());
            }
            
            builder.reason(reasonBuilder.build());
        }
        
        return builder.build();
    }
    
    @Override
    public void close() {
        try {
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.error("Error shutting down channel", e);
            Thread.currentThread().interrupt();
        }
    }
}

