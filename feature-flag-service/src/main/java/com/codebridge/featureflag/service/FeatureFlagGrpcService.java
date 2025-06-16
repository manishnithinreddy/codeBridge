package com.codebridge.featureflag.service;

import com.codebridge.featureflag.grpc.*;
import com.codebridge.featureflag.model.*;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * gRPC service implementation for the Feature Flag Service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FeatureFlagGrpcService extends FeatureFlagServiceGrpc.FeatureFlagServiceImplBase {
    
    private final FeatureFlagService featureFlagService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ChannelTopic flagUpdatesTopic;
    private final RedisMessageListenerContainer redisMessageListenerContainer;
    
    // Map to store active stream observers for flag updates
    private final Map<String, Set<StreamObserver<FlagUpdateEvent>>> flagStreamObservers = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void init() {
        // Set up Redis message listener for flag updates
        MessageListener messageListener = (message, pattern) -> {
            try {
                FeatureFlag flag = (FeatureFlag) redisTemplate.getValueSerializer().deserialize(message.getBody());
                if (flag != null) {
                    // Create update event
                    FlagUpdateEvent event = createFlagUpdateEvent(flag, EventType.UPDATED);
                    
                    // Notify all observers for this flag
                    notifyObservers(flag.getKey(), flag.getNamespace(), event);
                }
            } catch (Exception e) {
                log.error("Error processing flag update message", e);
            }
        };
        
        // Register the listener
        redisMessageListenerContainer.addMessageListener(messageListener, flagUpdatesTopic);
    }
    
    /**
     * Creates a flag update event from a feature flag.
     * 
     * @param flag the feature flag
     * @param eventType the event type
     * @return the flag update event
     */
    private FlagUpdateEvent createFlagUpdateEvent(FeatureFlag flag, EventType eventType) {
        FlagUpdateEvent.Builder eventBuilder = FlagUpdateEvent.newBuilder()
                .setEventType(eventType)
                .setFlagKey(flag.getKey())
                .setNamespace(flag.getNamespace())
                .setVersion(flag.getVersion())
                .setTimestamp(System.currentTimeMillis());
        
        // Set the value based on type
        if (flag.getValue() != null) {
            switch (flag.getValue().getType()) {
                case BOOLEAN:
                    eventBuilder.setBoolValue((Boolean) flag.getValue().getValue());
                    break;
                case STRING:
                    eventBuilder.setStringValue((String) flag.getValue().getValue());
                    break;
                case INTEGER:
                    eventBuilder.setIntValue(((Number) flag.getValue().getValue()).longValue());
                    break;
                case DOUBLE:
                    eventBuilder.setDoubleValue(((Number) flag.getValue().getValue()).doubleValue());
                    break;
                case JSON:
                    eventBuilder.setJsonValue((String) flag.getValue().getValue());
                    break;
            }
        }
        
        return eventBuilder.build();
    }
    
    /**
     * Notifies all observers for a flag.
     * 
     * @param flagKey the flag key
     * @param namespace the namespace
     * @param event the event
     */
    private void notifyObservers(String flagKey, String namespace, FlagUpdateEvent event) {
        // Notify observers for this specific flag
        String key = namespace + ":" + flagKey;
        if (flagStreamObservers.containsKey(key)) {
            Set<StreamObserver<FlagUpdateEvent>> observers = flagStreamObservers.get(key);
            Iterator<StreamObserver<FlagUpdateEvent>> iterator = observers.iterator();
            
            while (iterator.hasNext()) {
                StreamObserver<FlagUpdateEvent> observer = iterator.next();
                try {
                    observer.onNext(event);
                } catch (Exception e) {
                    log.error("Error notifying observer for flag {}", key, e);
                    iterator.remove();
                }
            }
        }
        
        // Notify observers for all flags in this namespace
        String namespaceKey = namespace + ":*";
        if (flagStreamObservers.containsKey(namespaceKey)) {
            Set<StreamObserver<FlagUpdateEvent>> observers = flagStreamObservers.get(namespaceKey);
            Iterator<StreamObserver<FlagUpdateEvent>> iterator = observers.iterator();
            
            while (iterator.hasNext()) {
                StreamObserver<FlagUpdateEvent> observer = iterator.next();
                try {
                    observer.onNext(event);
                } catch (Exception e) {
                    log.error("Error notifying observer for namespace {}", namespaceKey, e);
                    iterator.remove();
                }
            }
        }
    }
    
    @Override
    public void getFlag(FlagRequest request, StreamObserver<FlagResponse> responseObserver) {
        try {
            String flagKey = request.getFlagKey();
            String namespace = request.getNamespace();
            
            Optional<FeatureFlag> optionalFlag = featureFlagService.getFlag(flagKey, namespace);
            
            if (optionalFlag.isPresent()) {
                FeatureFlag flag = optionalFlag.get();
                FlagResponse response = convertToFlagResponse(flag);
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } else {
                FlagResponse response = FlagResponse.newBuilder()
                        .setFlagKey(flagKey)
                        .setExists(false)
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        } catch (Exception e) {
            log.error("Error getting flag", e);
            responseObserver.onError(new StatusRuntimeException(Status.INTERNAL.withDescription("Error getting flag: " + e.getMessage())));
        }
    }
    
    @Override
    public void getFlags(MultiFlagRequest request, StreamObserver<MultiFlagResponse> responseObserver) {
        try {
            List<String> flagKeys = request.getFlagKeysList();
            String namespace = request.getNamespace();
            
            List<FeatureFlag> flags = featureFlagService.getFlags(flagKeys, namespace);
            
            MultiFlagResponse.Builder responseBuilder = MultiFlagResponse.newBuilder();
            
            for (FeatureFlag flag : flags) {
                FlagResponse flagResponse = convertToFlagResponse(flag);
                responseBuilder.addFlags(flagResponse);
            }
            
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error getting flags", e);
            responseObserver.onError(new StatusRuntimeException(Status.INTERNAL.withDescription("Error getting flags: " + e.getMessage())));
        }
    }
    
    @Override
    public void evaluateFlag(FlagEvaluationRequest request, StreamObserver<FlagEvaluationResponse> responseObserver) {
        try {
            String flagKey = request.getFlagKey();
            String namespace = request.getNamespace();
            
            // Convert gRPC context to model context
            EvaluationContext context = convertToEvaluationContext(request.getContext());
            
            // Evaluate the flag
            EvaluationResult result = featureFlagService.evaluateFlag(flagKey, namespace, context);
            
            // Convert result to response
            FlagEvaluationResponse response = convertToFlagEvaluationResponse(result);
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error evaluating flag", e);
            responseObserver.onError(new StatusRuntimeException(Status.INTERNAL.withDescription("Error evaluating flag: " + e.getMessage())));
        }
    }
    
    @Override
    public void setFlag(SetFlagRequest request, StreamObserver<SetFlagResponse> responseObserver) {
        try {
            String flagKey = request.getFlagKey();
            String namespace = request.getNamespace();
            
            // Create flag from request
            FeatureFlag.Builder flagBuilder = FeatureFlag.builder()
                    .key(flagKey)
                    .namespace(namespace)
                    .description(request.getDescription())
                    .tags(request.getTagsMap())
                    .temporary(request.getTemporary())
                    .version(request.getVersion());
            
            // Set expiration time if provided
            if (request.getExpirationTime() > 0) {
                flagBuilder.expirationTime(Instant.ofEpochMilli(request.getExpirationTime()));
            }
            
            // Set value based on type
            if (request.hasBoolValue()) {
                flagBuilder.value(FlagValue.ofBoolean(request.getBoolValue()));
            } else if (request.hasStringValue()) {
                flagBuilder.value(FlagValue.ofString(request.getStringValue()));
            } else if (request.hasIntValue()) {
                flagBuilder.value(FlagValue.ofInteger(request.getIntValue()));
            } else if (request.hasDoubleValue()) {
                flagBuilder.value(FlagValue.ofDouble(request.getDoubleValue()));
            } else if (request.hasJsonValue()) {
                flagBuilder.value(FlagValue.ofJson(request.getJsonValue()));
            }
            
            // Save the flag
            FeatureFlag savedFlag = featureFlagService.setFlag(flagBuilder.build());
            
            // Create response
            SetFlagResponse response = SetFlagResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Flag saved successfully")
                    .setVersion(savedFlag.getVersion())
                    .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error setting flag", e);
            responseObserver.onError(new StatusRuntimeException(Status.INTERNAL.withDescription("Error setting flag: " + e.getMessage())));
        }
    }
    
    @Override
    public void streamFlagUpdates(FlagStreamRequest request, StreamObserver<FlagUpdateEvent> responseObserver) {
        try {
            List<String> flagKeys = request.getFlagKeysList();
            String namespace = request.getNamespace();
            
            // Register observer for each flag
            for (String flagKey : flagKeys) {
                String key = namespace + ":" + flagKey;
                flagStreamObservers.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet()).add(responseObserver);
            }
            
            // If no specific flags, register for all flags in the namespace
            if (flagKeys.isEmpty()) {
                String namespaceKey = namespace + ":*";
                flagStreamObservers.computeIfAbsent(namespaceKey, k -> ConcurrentHashMap.newKeySet()).add(responseObserver);
            }
            
            // Send initial values
            List<FeatureFlag> flags;
            if (flagKeys.isEmpty()) {
                flags = featureFlagService.listFlags(namespace, null, null, 0, 1000);
            } else {
                flags = featureFlagService.getFlags(flagKeys, namespace);
            }
            
            for (FeatureFlag flag : flags) {
                FlagUpdateEvent event = createFlagUpdateEvent(flag, EventType.CREATED);
                responseObserver.onNext(event);
            }
        } catch (Exception e) {
            log.error("Error streaming flag updates", e);
            responseObserver.onError(new StatusRuntimeException(Status.INTERNAL.withDescription("Error streaming flag updates: " + e.getMessage())));
        }
    }
    
    @Override
    public void listFlags(ListFlagsRequest request, StreamObserver<ListFlagsResponse> responseObserver) {
        try {
            String namespace = request.getNamespace();
            String prefix = request.getPrefix();
            String tagFilter = request.getTagFilter();
            int pageSize = request.getPageSize() > 0 ? request.getPageSize() : 10;
            
            // Parse page token
            int offset = 0;
            if (!request.getPageToken().isEmpty()) {
                try {
                    offset = Integer.parseInt(request.getPageToken());
                } catch (NumberFormatException e) {
                    log.warn("Invalid page token: {}", request.getPageToken());
                }
            }
            
            // Get flags
            List<FeatureFlag> flags = featureFlagService.listFlags(namespace, prefix, tagFilter, offset, pageSize);
            
            // Get total count
            int totalCount = featureFlagService.countFlags(namespace, prefix, tagFilter);
            
            // Create response
            ListFlagsResponse.Builder responseBuilder = ListFlagsResponse.newBuilder()
                    .setTotalCount(totalCount);
            
            // Add flags
            for (FeatureFlag flag : flags) {
                FlagSummary summary = convertToFlagSummary(flag);
                responseBuilder.addFlags(summary);
            }
            
            // Set next page token if there are more flags
            if (offset + pageSize < totalCount) {
                responseBuilder.setNextPageToken(String.valueOf(offset + pageSize));
            }
            
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error listing flags", e);
            responseObserver.onError(new StatusRuntimeException(Status.INTERNAL.withDescription("Error listing flags: " + e.getMessage())));
        }
    }
    
    @Override
    public void healthCheck(HealthCheckRequest request, StreamObserver<HealthCheckResponse> responseObserver) {
        try {
            HealthCheckResponse.Builder responseBuilder = HealthCheckResponse.newBuilder()
                    .setStatus(HealthCheckResponse.Status.SERVING)
                    .setVersion("1.0.0");
            
            // Add details
            Map<String, String> details = new HashMap<>();
            details.put("timestamp", String.valueOf(System.currentTimeMillis()));
            details.put("flags_count", String.valueOf(featureFlagService.countFlags(null, null, null)));
            
            responseBuilder.putAllDetails(details);
            
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error performing health check", e);
            responseObserver.onError(new StatusRuntimeException(Status.INTERNAL.withDescription("Error performing health check: " + e.getMessage())));
        }
    }
    
    /**
     * Converts a feature flag to a flag response.
     * 
     * @param flag the feature flag
     * @return the flag response
     */
    private FlagResponse convertToFlagResponse(FeatureFlag flag) {
        FlagResponse.Builder responseBuilder = FlagResponse.newBuilder()
                .setFlagKey(flag.getKey())
                .setExists(true);
        
        // Set metadata
        FlagMetadata.Builder metadataBuilder = FlagMetadata.newBuilder()
                .setDescription(flag.getDescription() != null ? flag.getDescription() : "")
                .setCreatedAt(flag.getCreatedAt() != null ? flag.getCreatedAt().toEpochMilli() : 0)
                .setUpdatedAt(flag.getUpdatedAt() != null ? flag.getUpdatedAt().toEpochMilli() : 0)
                .setCreatedBy(flag.getCreatedBy() != null ? flag.getCreatedBy() : "")
                .setUpdatedBy(flag.getUpdatedBy() != null ? flag.getUpdatedBy() : "")
                .setVersion(flag.getVersion() != null ? flag.getVersion() : "");
        
        // Add tags
        if (flag.getTags() != null) {
            metadataBuilder.putAllTags(flag.getTags());
        }
        
        responseBuilder.setMetadata(metadataBuilder.build());
        
        // Set value based on type
        if (flag.getValue() != null) {
            switch (flag.getValue().getType()) {
                case BOOLEAN:
                    responseBuilder.setBoolValue((Boolean) flag.getValue().getValue());
                    break;
                case STRING:
                    responseBuilder.setStringValue((String) flag.getValue().getValue());
                    break;
                case INTEGER:
                    responseBuilder.setIntValue(((Number) flag.getValue().getValue()).longValue());
                    break;
                case DOUBLE:
                    responseBuilder.setDoubleValue(((Number) flag.getValue().getValue()).doubleValue());
                    break;
                case JSON:
                    responseBuilder.setJsonValue((String) flag.getValue().getValue());
                    break;
            }
        }
        
        return responseBuilder.build();
    }
    
    /**
     * Converts a gRPC evaluation context to a model evaluation context.
     * 
     * @param context the gRPC evaluation context
     * @return the model evaluation context
     */
    private EvaluationContext convertToEvaluationContext(com.codebridge.featureflag.grpc.EvaluationContext context) {
        EvaluationContext.Builder contextBuilder = EvaluationContext.builder()
                .userId(context.getUserId())
                .sessionId(context.getSessionId())
                .attributes(context.getAttributesMap())
                .numericAttributes(context.getNumericAttributesMap())
                .booleanAttributes(context.getBooleanAttributesMap());
        
        // Set service context if provided
        if (context.hasServiceContext()) {
            ServiceContext serviceContext = context.getServiceContext();
            contextBuilder.serviceContext(EvaluationContext.ServiceContext.builder()
                    .serviceName(serviceContext.getServiceName())
                    .serviceVersion(serviceContext.getServiceVersion())
                    .instanceId(serviceContext.getInstanceId())
                    .environment(serviceContext.getEnvironment())
                    .metrics(serviceContext.getMetricsMap())
                    .build());
        }
        
        return contextBuilder.build();
    }
    
    /**
     * Converts an evaluation result to a flag evaluation response.
     * 
     * @param result the evaluation result
     * @return the flag evaluation response
     */
    private FlagEvaluationResponse convertToFlagEvaluationResponse(EvaluationResult result) {
        FlagEvaluationResponse.Builder responseBuilder = FlagEvaluationResponse.newBuilder()
                .setFlagKey(result.getFlagKey());
        
        // Set variation ID if available
        if (result.getVariationId() != null) {
            responseBuilder.setVariationId(result.getVariationId());
        }
        
        // Set rule ID if available
        if (result.getRuleId() != null) {
            responseBuilder.setRuleId(result.getRuleId());
        }
        
        // Set reason
        if (result.getReason() != null) {
            EvaluationReason.Builder reasonBuilder = EvaluationReason.newBuilder();
            
            // Set reason type
            switch (result.getReason().getType()) {
                case DEFAULT:
                    reasonBuilder.setType(ReasonType.DEFAULT);
                    break;
                case RULE_MATCH:
                    reasonBuilder.setType(ReasonType.RULE_MATCH);
                    break;
                case PREREQUISITE_FAILED:
                    reasonBuilder.setType(ReasonType.PREREQUISITE_FAILED);
                    break;
                case ERROR:
                    reasonBuilder.setType(ReasonType.ERROR);
                    break;
                case DISABLED:
                    reasonBuilder.setType(ReasonType.DISABLED);
                    break;
                case FALLTHROUGH:
                    reasonBuilder.setType(ReasonType.FALLTHROUGH);
                    break;
                case TARGET_MATCH:
                    reasonBuilder.setType(ReasonType.TARGET_MATCH);
                    break;
            }
            
            // Set rule ID if available
            if (result.getReason().getRuleId() != null) {
                reasonBuilder.setRuleId(result.getReason().getRuleId());
            }
            
            // Set description if available
            if (result.getReason().getDescription() != null) {
                reasonBuilder.setDescription(result.getReason().getDescription());
            }
            
            // Set error message if available
            if (result.getReason().getErrorMessage() != null) {
                reasonBuilder.setErrorMessage(result.getReason().getErrorMessage());
            }
            
            responseBuilder.setReason(reasonBuilder.build());
        }
        
        // Set value based on type
        if (result.getValue() != null) {
            switch (result.getValue().getType()) {
                case BOOLEAN:
                    responseBuilder.setBoolValue((Boolean) result.getValue().getValue());
                    break;
                case STRING:
                    responseBuilder.setStringValue((String) result.getValue().getValue());
                    break;
                case INTEGER:
                    responseBuilder.setIntValue(((Number) result.getValue().getValue()).longValue());
                    break;
                case DOUBLE:
                    responseBuilder.setDoubleValue(((Number) result.getValue().getValue()).doubleValue());
                    break;
                case JSON:
                    responseBuilder.setJsonValue((String) result.getValue().getValue());
                    break;
            }
        }
        
        return responseBuilder.build();
    }
    
    /**
     * Converts a feature flag to a flag summary.
     * 
     * @param flag the feature flag
     * @return the flag summary
     */
    private FlagSummary convertToFlagSummary(FeatureFlag flag) {
        FlagSummary.Builder summaryBuilder = FlagSummary.newBuilder()
                .setFlagKey(flag.getKey())
                .setNamespace(flag.getNamespace())
                .setDescription(flag.getDescription() != null ? flag.getDescription() : "")
                .setUpdatedAt(flag.getUpdatedAt() != null ? flag.getUpdatedAt().toEpochMilli() : 0)
                .setTemporary(flag.isTemporary());
        
        // Set value type
        if (flag.getValue() != null) {
            switch (flag.getValue().getType()) {
                case BOOLEAN:
                    summaryBuilder.setValueType(ValueType.BOOLEAN);
                    break;
                case STRING:
                    summaryBuilder.setValueType(ValueType.STRING);
                    break;
                case INTEGER:
                    summaryBuilder.setValueType(ValueType.INTEGER);
                    break;
                case DOUBLE:
                    summaryBuilder.setValueType(ValueType.DOUBLE);
                    break;
                case JSON:
                    summaryBuilder.setValueType(ValueType.JSON);
                    break;
            }
        }
        
        // Set expiration time if available
        if (flag.getExpirationTime() != null) {
            summaryBuilder.setExpirationTime(flag.getExpirationTime().toEpochMilli());
        }
        
        // Add tags
        if (flag.getTags() != null) {
            summaryBuilder.putAllTags(flag.getTags());
        }
        
        return summaryBuilder.build();
    }
}

