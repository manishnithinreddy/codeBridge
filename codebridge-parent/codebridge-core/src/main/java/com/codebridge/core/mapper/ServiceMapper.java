package com.codebridge.core.mapper;

import com.codebridge.core.dto.ServiceDto;
import com.codebridge.core.model.Service;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ServiceMapper {
    
    ServiceDto toDto(Service service);
    
    List<ServiceDto> toDtoList(List<Service> services);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "teamServices", ignore = true)
    Service toEntity(ServiceDto serviceDto);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "teamServices", ignore = true)
    void updateEntity(ServiceDto serviceDto, @MappingTarget Service service);
}

