package com.xcess.ocs.mapper;

import com.xcess.ocs.dto.RateDetailDTO;
import com.xcess.ocs.dto.RatePackageDTO;
import com.xcess.ocs.entity.Pulse;
import com.xcess.ocs.entity.RatePackage;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RatePackageMapper {

    public static RatePackageDTO toDTO(RatePackage entity) {
        RatePackageDTO dto = new RatePackageDTO();
        dto.setRatePackageId(entity.getRatePackageId());
        dto.setPackageName(entity.getPackageName());
        dto.setPackageDesc(entity.getPackageDesc());
        dto.setType(entity.getType());
        dto.setServiceType(entity.getServiceType());
        dto.setRatePackageType(entity.getRatePackageType());
        if (entity.getPulse() != null) {
            dto.setPulseId(entity.getPulse().getPulseId());
            dto.setPulseName(entity.getPulse().getPulseName());
        }
        dto.setRounding(entity.getRounding());
        dto.setPriceRounding(entity.getPriceRounding());
        dto.setCurrency(entity.getCurrency());
        dto.setStartTime(entity.getStartTime());
        dto.setEndTime(entity.getEndTime());
        dto.setRate(entity.getRate());


        List<RateDetailDTO> rateDetails = entity.getRateDetails().stream()
                .map(RateDetailMapper::toDTO)
                .collect(Collectors.toList());

        dto.setRate_details(rateDetails);
        return dto;
    }

    public static RatePackage toEntity(RatePackageDTO dto, Pulse pulse) {
        RatePackage entity = new RatePackage();
        entity.setRatePackageId(dto.getRatePackageId());
        entity.setPackageName(dto.getPackageName());
        entity.setPackageDesc(dto.getPackageDesc());
        entity.setType(dto.getType());
        entity.setServiceType(dto.getServiceType());
        entity.setRatePackageType(dto.getRatePackageType());
        entity.setPulse(pulse);
        entity.setRounding(dto.getRounding());
        entity.setPriceRounding(dto.getPriceRounding());
        entity.setCurrency(dto.getCurrency());
        entity.setStartTime(dto.getStartTime());
        entity.setEndTime(dto.getEndTime());
        entity.setRate(dto.getRate());

        entity.setRateDetails(new ArrayList<>());
        return entity;
    }
}