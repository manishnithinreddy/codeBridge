package com.codebridge.gitlab.git.mapper;

import com.codebridge.gitlab.git.dto.SharedStashDTO;
import com.codebridge.gitlab.git.model.SharedStash;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for converting between SharedStash entities and DTOs.
 */
@Component
public class SharedStashMapper {

    /**
     * Convert a SharedStash entity to a SharedStashDTO.
     *
     * @param sharedStash The SharedStash entity to convert
     * @return The corresponding SharedStashDTO
     */
    public SharedStashDTO toDTO(SharedStash sharedStash) {
        if (sharedStash == null) {
            return null;
        }
        
        SharedStashDTO dto = new SharedStashDTO();
        dto.setId(sharedStash.getId());
        dto.setStashHash(sharedStash.getStashHash());
        dto.setRepositoryId(sharedStash.getRepository().getId());
        dto.setRepositoryName(sharedStash.getRepository().getName());
        dto.setSharedBy(sharedStash.getSharedBy());
        dto.setSharedAt(sharedStash.getSharedAt());
        dto.setDescription(sharedStash.getDescription());
        dto.setBranch(sharedStash.getBranch());
        return dto;
    }
    
    /**
     * Convert a list of SharedStash entities to a list of SharedStashDTOs.
     *
     * @param sharedStashes The list of SharedStash entities to convert
     * @return The corresponding list of SharedStashDTOs
     */
    public List<SharedStashDTO> toDTOList(List<SharedStash> sharedStashes) {
        if (sharedStashes == null) {
            return List.of();
        }
        
        return sharedStashes.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
}
