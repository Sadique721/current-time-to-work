package com.xcess.ocs.dto;

import com.xcess.ocs.entity.RateDetails;
import com.xcess.ocs.entity.RatePackage;
import com.xcess.ocs.entity.ServiceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "19. Rate Card Response", description = "Response DTO for rate card details")
public class RateCardResponseDTO {

    @Schema(description = "Name of the rate package")
    private String packageName;

    @Schema(description = "Description of the rate package")
    private String packageDesc;

    @Schema(description = "Service type of the rate package", allowableValues = {"USAGE", "SMS", "VOICE"})
    @NotNull(message = "Service type is required")
    private ServiceType serviceType;

    @Schema(description = "List of rate details associated with the rate package")
    private List<RateDetailDTO> rateDetails;

    public static RateCardResponseDTO fromRatePackage(RatePackage ratePackage) {
        return RateCardResponseDTO.builder()
                .packageName(ratePackage.getPackageName())
                .packageDesc(ratePackage.getPackageDesc())
                .serviceType(ratePackage.getServiceType())
                .rateDetails(ratePackage.getRateDetails().stream()
                        .map(RateDetailDTO::fromEntity)
                        .collect(Collectors.toList()))
                .build();
    }

    public static RateCardResponseDTO fromRatePackageWithFilteredDetails(RatePackage ratePackage, List<RateDetails> filteredDetails) {
        return RateCardResponseDTO.builder()
                .packageName(ratePackage.getPackageName())
                .packageDesc(ratePackage.getPackageDesc())
                .serviceType(ratePackage.getServiceType())
                .rateDetails(filteredDetails.stream()
                        .map(RateDetailDTO::fromEntity)
                        .collect(Collectors.toList()))
                .build();
    }

}


