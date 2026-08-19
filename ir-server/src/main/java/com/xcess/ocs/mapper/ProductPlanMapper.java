package com.xcess.ocs.mapper;

import com.xcess.ocs.dto.ProductPlanAssociationDTO;
import com.xcess.ocs.dto.ProductPlanDTO;
import com.xcess.ocs.entity.ProductPlan;
import org.springframework.stereotype.Component;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.List;

@Component
public class ProductPlanMapper {

    public static ProductPlanDTO toDTO(ProductPlan entity) {
        if (entity == null) {
            return null;
        }

        List<ProductPlanAssociationDTO> associations = entity.getRatePackageGroups().stream()
                .map(assoc -> ProductPlanAssociationDTO.builder()
                        .ratePackageGroupId(assoc.getRatePackageGroup().getRatePackageGroupId())
                        .ratePackageGroupName(assoc.getRatePackageGroup().getName())
                        .serviceType(assoc.getServiceType() != null ? assoc.getServiceType().name() : null)
                        .build())
                .collect(Collectors.toList());

        return ProductPlanDTO.builder()
                .productPlanId(entity.getProductPlanId())
                .name(entity.getName())
                .description(entity.getDescription())
                .packageType(entity.getPackageType().getValue())
                .ratePackageGroups(associations)
                .build();
    }

    public static ProductPlan toEntity(ProductPlanDTO dto) {
        if (dto == null) {
            return null;
        }

        return ProductPlan.builder()
                .productPlanId(dto.getProductPlanId())
                .name(dto.getName())
                .description(dto.getDescription())
                .packageType(ProductPlan.PackageType.fromString(dto.getPackageType()))
                .ratePackageGroups(new ArrayList<>())
                .build();
    }
}