package com.codebridge.gitlab.git.service.impl;

import com.codebridge.gitlab.git.dto.GitProviderDto;
import com.codebridge.git.exception.ResourceNotFoundException;
import com.codebridge.gitlab.git.mapper.GitProviderMapper;
import com.codebridge.gitlab.git.model.GitProvider;
import com.codebridge.gitlab.git.model.GitProvider.ProviderType;
import com.codebridge.gitlab.git.repository.GitProviderRepository;
import com.codebridge.gitlab.git.service.GitProviderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GitProviderServiceImpl implements GitProviderService {
    
    private final GitProviderRepository gitProviderRepository;
    private final GitProviderMapper gitProviderMapper;
    
    @Override
    @Transactional(readOnly = true)
    public List<GitProviderDto> getAllProviders() {
        return gitProviderMapper.toDtoList(gitProviderRepository.findAll());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<GitProviderDto> getEnabledProviders() {
        return gitProviderMapper.toDtoList(gitProviderRepository.findByEnabled(true));
    }
    
    @Override
    @Transactional(readOnly = true)
    public GitProviderDto getProviderById(UUID id) {
        return gitProviderRepository.findById(id)
                .map(gitProviderMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Git provider not found with id: " + id));
    }
    
    @Override
    @Transactional(readOnly = true)
    public GitProviderDto getProviderByName(String name) {
        return gitProviderRepository.findByName(name)
                .map(gitProviderMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Git provider not found with name: " + name));
    }
    
    @Override
    @Transactional(readOnly = true)
    public GitProviderDto getProviderByType(ProviderType type) {
        return gitProviderRepository.findByTypeAndEnabled(type, true)
                .map(gitProviderMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("No enabled Git provider found with type: " + type));
    }
    
    @Override
    @Transactional
    public GitProviderDto createProvider(GitProviderDto providerDto) {
        GitProvider provider = gitProviderMapper.toEntity(providerDto);
        GitProvider savedProvider = gitProviderRepository.save(provider);
        log.info("Created Git provider: {}", savedProvider.getName());
        return gitProviderMapper.toDto(savedProvider);
    }
    
    @Override
    @Transactional
    public GitProviderDto updateProvider(UUID id, GitProviderDto providerDto) {
        GitProvider provider = gitProviderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Git provider not found with id: " + id));
        
        gitProviderMapper.updateEntity(providerDto, provider);
        GitProvider updatedProvider = gitProviderRepository.save(provider);
        log.info("Updated Git provider: {}", updatedProvider.getName());
        return gitProviderMapper.toDto(updatedProvider);
    }
    
    @Override
    @Transactional
    public void deleteProvider(UUID id) {
        GitProvider provider = gitProviderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Git provider not found with id: " + id));
        
        gitProviderRepository.delete(provider);
        log.info("Deleted Git provider: {}", provider.getName());
    }
    
    @Override
    @Transactional
    public void enableProvider(UUID id) {
        GitProvider provider = gitProviderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Git provider not found with id: " + id));
        
        provider.setEnabled(true);
        gitProviderRepository.save(provider);
        log.info("Enabled Git provider: {}", provider.getName());
    }
    
    @Override
    @Transactional
    public void disableProvider(UUID id) {
        GitProvider provider = gitProviderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Git provider not found with id: " + id));
        
        provider.setEnabled(false);
        gitProviderRepository.save(provider);
        log.info("Disabled Git provider: {}", provider.getName());
    }
}

