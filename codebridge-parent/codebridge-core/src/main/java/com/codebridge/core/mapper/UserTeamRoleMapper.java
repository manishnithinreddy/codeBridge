package com.codebridge.core.mapper;

import com.codebridge.core.dto.UserTeamRoleDto;
import com.codebridge.core.model.UserTeamRole;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;
import java.util.Set;

@Mapper(
    componentModel = "spring",
    uses = {UserMapper.class, TeamMapper.class, RoleMapper.class},
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface UserTeamRoleMapper {
    
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "teamId", source = "team.id")
    @Mapping(target = "roleId", source = "role.id")
    UserTeamRoleDto toDto(UserTeamRole userTeamRole);
    
    List<UserTeamRoleDto> toDtoList(List<UserTeamRole> userTeamRoles);
    
    Set<UserTeamRoleDto> toDtoSet(Set<UserTeamRole> userTeamRoles);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "team", ignore = true)
    @Mapping(target = "role", ignore = true)
    UserTeamRole toEntity(UserTeamRoleDto userTeamRoleDto);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "team", ignore = true)
    @Mapping(target = "role", ignore = true)
    void updateEntity(UserTeamRoleDto userTeamRoleDto, @MappingTarget UserTeamRole userTeamRole);
}

