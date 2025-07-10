package com.codebridge.core.mapper;

import com.codebridge.core.dto.TokenDto;
import com.codebridge.core.model.Token;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface TokenMapper {
    
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "teamId", source = "team.id")
    TokenDto toDto(Token token);
    
    List<TokenDto> toDtoList(List<Token> tokens);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "team", ignore = true)
    Token toEntity(TokenDto tokenDto);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "team", ignore = true)
    void updateEntity(TokenDto tokenDto, @MappingTarget Token token);
}

