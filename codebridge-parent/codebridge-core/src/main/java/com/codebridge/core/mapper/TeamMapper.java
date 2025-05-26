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
    uses = {UserMapper.class, TeamServiceMapper.class},
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface TeamMapper {
    
    @Mapping(target = "parentTeamId", source = "parentTeam.id")
    @Mapping(target = "childTeams", qualifiedByName = "toTeamSummaryDto")
    TeamDto toDto(Team team);
    
    List<TeamDto> toDtoList(List<Team> teams);
    
    @Named("toTeamSummaryDto")
    @Mapping(target = "parentTeamId", source = "parentTeam.id")
    @Mapping(target = "ownerId", source = "owner.id")
    TeamSummaryDto toSummaryDto(Team team);
    
    @Named("toTeamSummaryDto")
    Set<TeamSummaryDto> toSummaryDtoSet(Set<Team> teams);
    
    List<TeamSummaryDto> toSummaryDtoList(List<Team> teams);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "parentTeam", ignore = true)
    @Mapping(target = "childTeams", ignore = true)
    @Mapping(target = "members", ignore = true)
    @Mapping(target = "teamServices", ignore = true)
    @Mapping(target = "teamRoles", ignore = true)
    Team toEntity(TeamDto teamDto);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "parentTeam", ignore = true)
    @Mapping(target = "childTeams", ignore = true)
    @Mapping(target = "members", ignore = true)
    @Mapping(target = "teamServices", ignore = true)
    @Mapping(target = "teamRoles", ignore = true)
    void updateEntity(TeamDto teamDto, @MappingTarget Team team);
}

