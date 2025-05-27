package com.codebridge.core.mapper;

import com.codebridge.core.dto.AuditLogDto;
import com.codebridge.core.model.AuditLog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface AuditLogMapper {
    
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "teamId", source = "team.id")
    @Mapping(target = "teamName", source = "team.name")
    AuditLogDto toDto(AuditLog auditLog);
    
    List<AuditLogDto> toDtoList(List<AuditLog> auditLogs);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "team", ignore = true)
    AuditLog toEntity(AuditLogDto auditLogDto);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "team", ignore = true)
    void updateEntity(AuditLogDto auditLogDto, @MappingTarget AuditLog auditLog);
}

