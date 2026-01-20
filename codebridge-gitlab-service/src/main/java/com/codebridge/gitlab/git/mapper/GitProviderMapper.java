package com.codebridge.gitlab.git.mapper;

import com.codebridge.gitlab.git.dto.GitProviderDto;
import com.codebridge.gitlab.git.model.GitProvider;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface GitProviderMapper {
    
    GitProviderDto toDto(GitProvider entity);
    
    List<GitProviderDto> toDtoList(List<GitProvider> entities);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "repositories", ignore = true)
    GitProvider toEntity(GitProviderDto dto);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "repositories", ignore = true)
    void updateEntity(GitProviderDto dto, @MappingTarget GitProvider entity);
}

