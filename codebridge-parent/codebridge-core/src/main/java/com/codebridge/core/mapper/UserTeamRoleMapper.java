package com.codebridge.core.mapper;

import com.codebridge.core.dto.UserTeamRoleDto;
import com.codebridge.core.model.UserTeamRole;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface UserTeamRoleMapper {
    
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "teamId", source = "team.id")
    @Mapping(target = "teamName", source = "team.name")
    @Mapping(target = "roleId", source = "role.id")
    @Mapping(target = "roleName", source = "role.name")
    UserTeamRoleDto toDto(UserTeamRole userTeamRole);
    
    List<UserTeamRoleDto> toDtoList(List<UserTeamRole> userTeamRoles);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "team", ignore = true)
    @Mapping(target = "role", ignore = true)
    UserTeamRole toEntity(UserTeamRoleDto userTeamRoleDto);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "team", ignore = true)
    @Mapping(target = "role", ignore = true)
    void updateEntity(UserTeamRoleDto userTeamRoleDto, @MappingTarget UserTeamRole userTeamRole);
}

