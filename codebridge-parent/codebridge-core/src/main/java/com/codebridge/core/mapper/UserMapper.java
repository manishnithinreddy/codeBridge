package com.codebridge.core.mapper;

import com.codebridge.core.dto.UserDto;
import com.codebridge.core.dto.UserSummaryDto;
import com.codebridge.core.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(
    componentModel = "spring",
    uses = {TeamMapper.class},
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface UserMapper {
    
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "teams", qualifiedByName = "toTeamSummaryDto")
    UserDto toDto(User user);
    
    List<UserDto> toDtoList(List<User> users);
    
    UserSummaryDto toSummaryDto(User user);
    
    List<UserSummaryDto> toSummaryDtoList(List<User> users);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "teams", ignore = true)
    User toEntity(UserDto userDto);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "teams", ignore = true)
    void updateEntity(UserDto userDto, @MappingTarget User user);
}

