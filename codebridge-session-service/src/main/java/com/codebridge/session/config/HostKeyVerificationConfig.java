package com.codebridge.session.config;

import com.codebridge.session.service.CustomJschHostKeyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Configuration for SSH host key verification
 */
@Configuration
public class HostKeyVerificationConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(HostKeyVerificationConfig.class);
    
    private final CustomJschHostKeyRepository customJschHostKeyRepository;
    
    @Value("${codebridge.ssh.host-key-verification-policy:AUTO_ACCEPT}")
    private String hostKeyVerificationPolicy;
    
    public HostKeyVerificationConfig(CustomJschHostKeyRepository customJschHostKeyRepository) {
        this.customJschHostKeyRepository = customJschHostKeyRepository;
    }
    
    @PostConstruct
    public void init() {
        try {
            CustomJschHostKeyRepository.HostKeyVerificationPolicy policy = 
                    CustomJschHostKeyRepository.HostKeyVerificationPolicy.valueOf(hostKeyVerificationPolicy);
            
            customJschHostKeyRepository.setVerificationPolicy(policy);
            logger.info("Host key verification policy set to: {}", policy);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid host key verification policy: {}. Using default AUTO_ACCEPT.", hostKeyVerificationPolicy);
            customJschHostKeyRepository.setVerificationPolicy(
                    CustomJschHostKeyRepository.HostKeyVerificationPolicy.AUTO_ACCEPT);
        }
    }
}

