package com.codebridge.core.mapper;

import com.codebridge.core.dto.RoleDto;
import com.codebridge.core.model.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(
    componentModel = "spring",
    uses = {PermissionMapper.class},
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface RoleMapper {
    
    RoleDto toDto(Role role);
    
    List<RoleDto> toDtoList(List<Role> roles);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "permissions", ignore = true)
    @Mapping(target = "userTeamRoles", ignore = true)
    Role toEntity(RoleDto roleDto);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "permissions", ignore = true)
    @Mapping(target = "userTeamRoles", ignore = true)
    void updateEntity(RoleDto roleDto, @MappingTarget Role role);
}

