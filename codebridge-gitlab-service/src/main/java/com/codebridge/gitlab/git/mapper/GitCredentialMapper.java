package com.codebridge.gitlab.git.mapper;

import com.codebridge.gitlab.git.dto.GitCredentialDto;
import com.codebridge.gitlab.git.model.GitCredential;
import com.codebridge.gitlab.git.model.GitProvider;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface GitCredentialMapper {
    
    @Mapping(target = "providerId", source = "provider.id")
    @Mapping(target = "providerName", source = "provider.name")
    GitCredentialDto toDto(GitCredential entity);
    
    List<GitCredentialDto> toDtoList(List<GitCredential> entities);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "provider", ignore = true)
    GitCredential toEntity(GitCredentialDto dto);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "provider", ignore = true)
    void updateEntity(GitCredentialDto dto, @MappingTarget GitCredential entity);
    
    default GitCredential fromDto(GitCredentialDto dto, GitProvider provider) {
        GitCredential entity = toEntity(dto);
        entity.setProvider(provider);
        return entity;
    }
}

