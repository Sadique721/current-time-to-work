package com.xcess.ocs.mapper;

import com.xcess.ocs.dto.ZoneDTO;
import com.xcess.ocs.entity.PrefixInputMode;
import com.xcess.ocs.entity.Zone;
import org.springframework.stereotype.Component;

@Component
public class ZoneMapper {

    public ZoneDTO toDto(Zone zone) {
        if (zone == null) return null;
        ZoneDTO dto = new ZoneDTO();
        dto.setZoneId(zone.getZoneId());
        dto.setZoneName(zone.getZoneName());
        dto.setPrefixPattern(zone.getPrefixPattern());
        dto.setDescription(zone.getDescription());
        dto.setPriority(zone.getPriority());
        dto.setPrefixInputMode(zone.getPrefixInputMode());
        // selectedPrefixIds / selectedCountryIds / rawPrefixPattern are request-only — not returned
        return dto;
    }

    public Zone toEntity(ZoneDTO dto) {
        if (dto == null) return null;
        return Zone.builder()
                .zoneId(dto.getZoneId())
                .zoneName(dto.getZoneName())
                .prefixPattern(dto.getPrefixPattern())
                .description(dto.getDescription())
                .priority(dto.getPriority() != null ? dto.getPriority() : 100)
                .prefixInputMode(dto.getPrefixInputMode() != null ? dto.getPrefixInputMode() : PrefixInputMode.MANUAL)
                .build();
    }
}
