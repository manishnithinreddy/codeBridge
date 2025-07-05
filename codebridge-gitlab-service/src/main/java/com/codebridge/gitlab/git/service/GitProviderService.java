package com.codebridge.gitlab.git.service;

import com.codebridge.gitlab.git.dto.GitProviderDto;
import com.codebridge.gitlab.git.model.GitProvider.ProviderType;

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

