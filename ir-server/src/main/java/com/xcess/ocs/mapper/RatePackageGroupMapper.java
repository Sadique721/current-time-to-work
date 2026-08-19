package com.xcess.ocs.mapper;

import com.xcess.ocs.dto.RatePackageAssociationDTO;
import com.xcess.ocs.dto.RatePackageGroupDTO;

import com.xcess.ocs.entity.RatePackageGroup;
import org.springframework.stereotype.Component;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.List;

@Component
public class RatePackageGroupMapper {

    public static RatePackageGroupDTO toDTO(RatePackageGroup entity) {
        if (entity == null) {
            return null;
        }

        List<RatePackageAssociationDTO> associations = entity.getRatePackageAssociations().stream()
                .map(assoc -> {
                    RatePackageAssociationDTO dto = new RatePackageAssociationDTO();
                    dto.setRatePackage(assoc.getRatePackage().getRatePackageId());
                    dto.setPackageName(assoc.getRatePackage().getPackageName());
                    dto.setStartTime(assoc.getStartTime());
                    dto.setEndTime(assoc.getEndTime());
                    dto.setPriority(assoc.getPriority());
                    dto.setCallType(assoc.getCallType());
                    dto.setExpression(assoc.getExpression());
                    return dto;
                })
                .collect(Collectors.toList());

        return RatePackageGroupDTO.builder()
                .ratePackageGroupId(entity.getRatePackageGroupId())
                .ratePackageGroupName(entity.getName())
                .description(entity.getDescription())
                .packageType(entity.getPackageType().getValue())
                .serviceType(entity.getServiceType())

                .ratePackageSelectionType(entity.getRatePackageSelectionType() != null ? entity.getRatePackageSelectionType().name() : null)
                .ratePackages(associations)
                .build();
    }

    public static RatePackageGroup toEntity(RatePackageGroupDTO dto) {
        if (dto == null) {
            return null;
        }

        return RatePackageGroup.builder()
                .ratePackageGroupId(dto.getRatePackageGroupId())
                .name(dto.getRatePackageGroupName())
                .description(dto.getDescription())
                .packageType(RatePackageGroup.PackageType.fromString(dto.getPackageType()))

                .serviceType(dto.getServiceType())
                .ratePackageSelectionType(dto.getRatePackageSelectionType() != null ? RatePackageGroup.RatePackageSelectionType.valueOf(dto.getRatePackageSelectionType()) : null)
                .ratePackageAssociations(new ArrayList<>())
                .build();
    }
}