package com.codebridge.core.mapper;

import com.codebridge.core.dto.PermissionDto;
import com.codebridge.core.model.Permission;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;
import java.util.Set;

@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface PermissionMapper {
    
    PermissionDto toDto(Permission permission);
    
    List<PermissionDto> toDtoList(List<Permission> permissions);
    
    Set<PermissionDto> toDtoSet(Set<Permission> permissions);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "roles", ignore = true)
    Permission toEntity(PermissionDto permissionDto);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "roles", ignore = true)
    void updateEntity(PermissionDto permissionDto, @MappingTarget Permission permission);
}

