package com.xcess.ocs.mapper;

import com.xcess.ocs.dto.OrganizationDTO;
import com.xcess.ocs.entity.Organization;
import org.springframework.stereotype.Component;

/**
 * Mapper class for converting between Organization entity and OrganizationDTO.
 * Provides bidirectional mapping between database entity and API data transfer object.
 * 
 * @see Organization
 * @see OrganizationDTO
 */
@Component
public class OrganizationMapper {
    
    /**
     * Converts Organization entity to OrganizationDTO.
     * 
     * @param entity the Organization entity
     * @return OrganizationDTO or null if entity is null
     */
    public OrganizationDTO toDTO(Organization entity) {
        if (entity == null) return null;
        OrganizationDTO dto = new OrganizationDTO();
        dto.setOrganizationId(entity.getOrganizationId());
        dto.setName(entity.getName());
        dto.setSuffixName(entity.getSuffixName());
        dto.setLegalName(entity.getLegalName());
        dto.setAddress(entity.getAddress());
        return dto;
    }

    /**
     * Converts OrganizationDTO to Organization entity.
     * 
     * @param dto the OrganizationDTO
     * @return Organization entity or null if dto is null
     */
    public Organization toEntity(OrganizationDTO dto) {
        if (dto == null) return null;
        Organization entity = new Organization();
        entity.setOrganizationId(dto.getOrganizationId());
        entity.setName(dto.getName());
        entity.setSuffixName(dto.getSuffixName());
        entity.setLegalName(dto.getLegalName());
        entity.setAddress(dto.getAddress());
        return entity;
    }
}
