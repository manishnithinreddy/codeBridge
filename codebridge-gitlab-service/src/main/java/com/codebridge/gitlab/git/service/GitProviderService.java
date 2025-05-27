package com.codebridge.git.service;

import com.codebridge.git.dto.GitProviderDto;
import com.codebridge.git.model.GitProvider.ProviderType;

import java.util.List;
import java.util.UUID;

public interface GitProviderService {
    
    List<GitProviderDto> getAllProviders();
    
    List<GitProviderDto> getEnabledProviders();
    
    GitProviderDto getProviderById(UUID id);
    
    GitProviderDto getProviderByName(String name);
    
    GitProviderDto getProviderByType(ProviderType type);
    
    GitProviderDto createProvider(GitProviderDto providerDto);
    
    GitProviderDto updateProvider(UUID id, GitProviderDto providerDto);
    
    void deleteProvider(UUID id);
    
    void enableProvider(UUID id);
    
    void disableProvider(UUID id);
}

