package com.xcess.ocs.dto;

import com.xcess.ocs.entity.ServiceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "11. Rate Package Group", description = "Schema to hold Rate package group details")
public class RatePackageGroupDTO {

    @Schema(description = "Rate package group ID", accessMode = Schema.AccessMode.READ_ONLY)
    private Long ratePackageGroupId;

    @Schema(description = "Rate package group name", example = "ROAMING_MO_VOICE")
    @NotEmpty(message = "Rate package group name is required")
    private String ratePackageGroupName;

    @Schema(description = "Description of the rate package group", example = "MO Voice roaming group")
    @NotEmpty(message = "Description is required")
    private String description;

    @Schema(description = "Type of the package", example = "SELLING")
    @NotNull(message = "Package type is required")
    private String packageType;

    @Schema(description = "ROAMING: service type this group handles (VOICE, SMS, USAGE). Null for INTERCONNECT.", example = "VOICE")
    private ServiceType serviceType;



    @Schema(description = "Rate packages associated with the rate package group")
    @NotEmpty(message = "At least one rate package is required")
    private List<RatePackageAssociationDTO> ratePackages;

    @Schema(description = "Selection strategy: PRIORITY (ordered fallback) or CALL_TYPE (matched by call type for ROAMING)", example = "PRIORITY")
    @NotNull(message = "Rate package selection type is required")
    private String ratePackageSelectionType;
}