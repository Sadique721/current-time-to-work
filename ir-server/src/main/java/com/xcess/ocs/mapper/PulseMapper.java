package com.xcess.ocs.mapper;

import com.xcess.ocs.dto.PulseDTO;
import com.xcess.ocs.entity.Pulse;
import org.springframework.stereotype.Component;

@Component
public class PulseMapper {

    public static PulseDTO toDTO(Pulse entity) {
        if (entity == null) {
            return null;
        }

        return PulseDTO.builder()
                .pulseId(entity.getPulseId())
                .pulseName(entity.getPulseName())
                .serviceType(entity.getServiceType())
                .unit(entity.getUnit())
                .noOfUnits(entity.getNoOfUnits())
                .build();
    }

    public static Pulse toEntity(PulseDTO dto) {
        if (dto == null) {
            return null;
        }

        return new Pulse(
                dto.getPulseId(),
                dto.getPulseName(),
                dto.getServiceType(),
                dto.getUnit(),
                dto.getNoOfUnits()
        );
    }
}
