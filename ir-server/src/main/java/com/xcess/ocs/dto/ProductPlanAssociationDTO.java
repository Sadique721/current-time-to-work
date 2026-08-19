package com.xcess.ocs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "13. Product Plan Association", description = "Association between a product plan and a rate package group")
public class ProductPlanAssociationDTO {
    @Schema(description = "Rate Package Group ID", example = "1")
    @NotNull(message = "Rate Package Group ID is required")
    private Long ratePackageGroupId;

    @Schema(description = "Name of the Rate Package Group", example = "Premium Voice & SMS",accessMode = Schema.AccessMode.READ_ONLY)
    private String ratePackageGroupName;

    @Schema(description = "Service type: VOICE, SMS, or USAGE", example = "VOICE")
    @NotBlank(message = "Service type is required")
    private String serviceType;
}