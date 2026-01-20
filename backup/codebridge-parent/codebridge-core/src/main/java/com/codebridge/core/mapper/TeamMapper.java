package com.codebridge.core.mapper;

import com.codebridge.core.dto.TeamDto;
import com.codebridge.core.dto.TeamSummaryDto;
import com.codebridge.core.model.Team;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;
import java.util.Set;

@Mapper(
    componentModel = "spring",
    uses = {UserTeamRoleMapper.class, TeamServiceMapper.class},
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface TeamMapper {
    
    TeamDto toDto(Team team);
    
    List<TeamDto> toDtoList(List<Team> teams);
    
    @Named("toTeamSummaryDto")
    TeamSummaryDto toSummaryDto(Team team);
    
    @Named("toTeamSummaryDto")
    Set<TeamSummaryDto> toSummaryDtoSet(Set<Team> teams);
    
    List<TeamSummaryDto> toSummaryDtoList(List<Team> teams);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "userTeamRoles", ignore = true)
    @Mapping(target = "teamServices", ignore = true)
    Team toEntity(TeamDto teamDto);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "userTeamRoles", ignore = true)
    @Mapping(target = "teamServices", ignore = true)
    void updateEntity(TeamDto teamDto, @MappingTarget Team team);
}

