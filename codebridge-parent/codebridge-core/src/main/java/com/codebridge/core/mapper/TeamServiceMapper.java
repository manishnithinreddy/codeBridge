package com.codebridge.core.mapper;

import com.codebridge.core.dto.TeamServiceDto;
import com.codebridge.core.model.TeamService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;
import java.util.Set;

@Mapper(
    componentModel = "spring",
    uses = {ServiceMapper.class},
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface TeamServiceMapper {
    
    @Mapping(target = "teamId", source = "team.id")
    @Mapping(target = "serviceId", source = "service.id")
    TeamServiceDto toDto(TeamService teamService);
    
    List<TeamServiceDto> toDtoList(List<TeamService> teamServices);
    
    Set<TeamServiceDto> toDtoSet(Set<TeamService> teamServices);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "team", ignore = true)
    @Mapping(target = "service", ignore = true)
    TeamService toEntity(TeamServiceDto teamServiceDto);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "team", ignore = true)
    @Mapping(target = "service", ignore = true)
    void updateEntity(TeamServiceDto teamServiceDto, @MappingTarget TeamService teamService);
}

