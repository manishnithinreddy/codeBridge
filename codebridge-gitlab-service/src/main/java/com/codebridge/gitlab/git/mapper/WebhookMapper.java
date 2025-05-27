package com.codebridge.git.mapper;

import com.codebridge.git.dto.WebhookDto;
import com.codebridge.git.model.Repository;
import com.codebridge.git.model.Webhook;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface WebhookMapper {
    
    @Mapping(target = "repositoryId", source = "repository.id")
    @Mapping(target = "repositoryName", source = "repository.name")
    WebhookDto toDto(Webhook entity);
    
    List<WebhookDto> toDtoList(List<Webhook> entities);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "repository", ignore = true)
    Webhook toEntity(WebhookDto dto);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "repository", ignore = true)
    void updateEntity(WebhookDto dto, @MappingTarget Webhook entity);
    
    default Webhook fromDto(WebhookDto dto, Repository repository) {
        Webhook entity = toEntity(dto);
        entity.setRepository(repository);
        return entity;
    }
}

