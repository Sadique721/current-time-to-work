package com.xcess.ocs.mapper;

import com.xcess.ocs.dto.SourceCdrConfigurationDTO;
import com.xcess.ocs.entity.SourceCdrConfiguration;
import com.xcess.ocs.entity.SourceConfiguration;
import org.springframework.stereotype.Component;

@Component
public class SourceCdrConfigurationMapper {

    public SourceCdrConfiguration toEntity(SourceCdrConfigurationDTO dto) {
        if (dto == null) return null;

        SourceCdrConfiguration entity = new SourceCdrConfiguration();

        SourceConfiguration sourceConfig = new SourceConfiguration();
        sourceConfig.setSourceId(dto.getSourceId());
        entity.setSourceConfiguration(sourceConfig);

        entity.setSourceCdrConfigurationId(dto.getId());
        entity.setFieldName(dto.getFieldName());
        entity.setSequence(dto.getSequence());

        return entity;
    }

    public SourceCdrConfigurationDTO toDTO(SourceCdrConfiguration entity) {
        if (entity == null) return null;

        SourceCdrConfigurationDTO dto = new SourceCdrConfigurationDTO();

        dto.setId(entity.getSourceCdrConfigurationId());
        dto.setFieldName(entity.getFieldName());
        dto.setSequence(entity.getSequence());

        if (entity.getSourceConfiguration() != null) {
            dto.setSourceId(entity.getSourceConfiguration().getSourceId());
        }

        return dto;
    }
}
