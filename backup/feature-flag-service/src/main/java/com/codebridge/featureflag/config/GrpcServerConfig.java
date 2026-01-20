package com.codebridge.featureflag.config;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * gRPC server configuration for the Feature Flag Service.
 * Sets up the gRPC server with the appropriate services.
 */
@Configuration
@Slf4j
public class GrpcServerConfig {

    @Value("${grpc.server.port}")
    private int grpcPort;

    /**
     * Creates and configures the gRPC server.
     * 
     * @param featureFlagService The feature flag service implementation
     * @return The configured gRPC server
     * @throws IOException If the server cannot be created
     */
    @Bean
    public Server grpcServer(com.codebridge.featureflag.service.FeatureFlagGrpcService featureFlagService) throws IOException {
        Server server = ServerBuilder.forPort(grpcPort)
                .addService(featureFlagService)
                .build();
        
        // Start the server in a separate thread
        Thread grpcThread = new Thread(() -> {
            try {
                server.start();
                log.info("gRPC server started on port {}", grpcPort);
                server.awaitTermination();
            } catch (IOException e) {
                log.error("Failed to start gRPC server", e);
            } catch (InterruptedException e) {
                log.error("gRPC server interrupted", e);
            }
        });
        grpcThread.setDaemon(true);
        grpcThread.start();
        
        // Add shutdown hook to stop the server gracefully
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down gRPC server");
            server.shutdown();
        }));
        
        return server;
    }
}

