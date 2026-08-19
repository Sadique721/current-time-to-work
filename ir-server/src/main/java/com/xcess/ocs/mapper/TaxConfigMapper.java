package com.xcess.ocs.mapper;

import com.xcess.ocs.dto.TaxConfigDTO;
import com.xcess.ocs.entity.TaxConfig;
import org.springframework.stereotype.Component;

/**
 * Mapper class for converting between TaxConfig entity and TaxConfigDTO.
 * Provides bidirectional mapping between database entity and API data transfer object.
 *
 * @see TaxConfig
 * @see TaxConfigDTO
 */
@Component
public class TaxConfigMapper {

    /**
     * Converts TaxConfig entity to TaxConfigDTO.
     *
     * @param entity the TaxConfig entity
     * @return TaxConfigDTO or null if entity is null
     */
    public TaxConfigDTO toDTO(TaxConfig entity) {
        if (entity == null) {
            return null;
        }
        TaxConfigDTO dto = new TaxConfigDTO();
        dto.setTaxConfigId(entity.getTaxConfigId());
        dto.setTaxType(entity.getTaxType());
        dto.setTaxName(entity.getTaxName());
        dto.setStandardRate(entity.getStandardRate());
        dto.setAllowsInputCredit(entity.getAllowsInputCredit());
        dto.setIsActive(entity.getIsActive());
        dto.setEffectiveFrom(entity.getEffectiveFrom());
        dto.setEffectiveTo(entity.getEffectiveTo());
        dto.setApplyOn(entity.getApplyOn());
        return dto;
    }

    /**
     * Converts TaxConfigDTO to TaxConfig entity.
     *
     * @param dto the TaxConfigDTO
     * @return TaxConfig entity or null if dto is null
     */
    public TaxConfig toEntity(TaxConfigDTO dto) {
        if (dto == null) {
            return null;
        }
        TaxConfig entity = new TaxConfig();
        entity.setTaxConfigId(dto.getTaxConfigId());
        entity.setTaxType(dto.getTaxType());
        entity.setTaxName(dto.getTaxName());
        entity.setStandardRate(dto.getStandardRate());
        entity.setAllowsInputCredit(dto.getAllowsInputCredit());
        entity.setIsActive(dto.getIsActive());
        entity.setEffectiveFrom(dto.getEffectiveFrom());
        entity.setEffectiveTo(dto.getEffectiveTo());
        entity.setApplyOn(dto.getApplyOn());
        return entity;
    }
}
