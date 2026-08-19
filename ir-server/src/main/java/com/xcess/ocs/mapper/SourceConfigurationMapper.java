package com.xcess.ocs.mapper;

import com.xcess.ocs.dto.SourceConfigurationDTO;
import com.xcess.ocs.entity.SourceConfiguration;
import org.springframework.stereotype.Component;

@Component
public class SourceConfigurationMapper {

    public SourceConfiguration toEntity(SourceConfigurationDTO dto) {
        if (dto == null) return null;

        SourceConfiguration entity = new SourceConfiguration();
        entity.setSourceId(dto.getSourceId());
        entity.setSourceName(dto.getSourceName());
        entity.setTopicName(dto.getTopicName());
        entity.setStatus(dto.getStatus());
        if (dto.getServiceType() != null) {
            entity.setServiceType(dto.getServiceType());
        }
        return entity;
    }

    public SourceConfigurationDTO toDTO(SourceConfiguration entity) {
        if (entity == null) return null;

        SourceConfigurationDTO dto = new SourceConfigurationDTO();
        dto.setSourceId(entity.getSourceId());
        dto.setSourceName(entity.getSourceName());
        dto.setTopicName(entity.getTopicName());
        dto.setStatus(entity.getStatus());
        dto.setServiceType(entity.getServiceType());
        return dto;
    }
}
