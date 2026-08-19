package com.xcess.ocs.mapper;

import com.xcess.ocs.dto.PrefixDTO;
import com.xcess.ocs.entity.Prefix;
import com.xcess.ocs.entity.Country;
import org.springframework.stereotype.Component;

/**
 * Mapper class for converting between Prefix entity and PrefixDTO.
 * Provides bidirectional mapping between database entity and API data transfer object.
 * 
 * @see Prefix
 * @see PrefixDTO
 */
@Component
public class PrefixMapper {

    /**
     * Converts Prefix entity to PrefixDTO.
     * 
     * @param prefix the Prefix entity
     * @return PrefixDTO or null if prefix is null
     */
    public PrefixDTO toDto(Prefix prefix) {
        if (prefix == null) {
            return null;
        }

        PrefixDTO dto = new PrefixDTO();
        dto.setPrefixId(prefix.getPrefixId());
        dto.setPrefix(prefix.getPrefix());
        dto.setPrefixName(prefix.getPrefixName());
        dto.setPrefixType(prefix.getPrefixType());

        if (prefix.getCountry() != null) {
            dto.setCountryName(prefix.getCountry().getName());
        }

        return dto;
    }

    /**
     * Converts PrefixDTO to Prefix entity.
     * 
     * @param dto the PrefixDTO
     * @param country the Country entity to associate with prefix
     * @return Prefix entity or null if dto is null
     */
    public Prefix toEntity(PrefixDTO dto, Country country) {
        if (dto == null) {
            return null;
        }

        Prefix prefix = new Prefix();
        prefix.setPrefixId(dto.getPrefixId());
        prefix.setPrefix(dto.getPrefix());
        prefix.setPrefixName(dto.getPrefixName());
        prefix.setCountry(country);
        prefix.setPrefixType(dto.getPrefixType() != null ? dto.getPrefixType() : com.xcess.ocs.entity.PrefixType.INTERCONNECT);

        return prefix;
    }
}