package com.codebridge.git.mapper;

import com.codebridge.git.dto.RepositoryDto;
import com.codebridge.git.model.GitProvider;
import com.codebridge.git.model.Repository;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RepositoryMapper {
    
    @Mapping(target = "providerId", source = "provider.id")
    @Mapping(target = "providerName", source = "provider.name")
    RepositoryDto toDto(Repository entity);
    
    List<RepositoryDto> toDtoList(List<Repository> entities);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "provider", ignore = true)
    @Mapping(target = "webhooks", ignore = true)
    Repository toEntity(RepositoryDto dto);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "provider", ignore = true)
    @Mapping(target = "webhooks", ignore = true)
    void updateEntity(RepositoryDto dto, @MappingTarget Repository entity);
    
    default Repository fromDto(RepositoryDto dto, GitProvider provider) {
        Repository entity = toEntity(dto);
        entity.setProvider(provider);
        return entity;
    }
}

