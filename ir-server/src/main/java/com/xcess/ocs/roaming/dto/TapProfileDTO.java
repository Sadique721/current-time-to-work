package com.xcess.ocs.roaming.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "DTO for TAP profile — used for both request and response")
public class TapProfileDTO {

    @Schema(description = "Unique identifier (populated in response only)", example = "101")
    private Long id;

    @NotBlank
    @Schema(description = "Unique name identifying this profile", example = "EU_Standard_Profile",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String profileName;

    @Schema(description = "Human-readable description of the profile's use case",
            example = "Standard field mappings for European Union roaming partners")
    private String description;

    @Schema(description = "Whether this profile is active and available for assignment", example = "true")
    private boolean isActive = true;

    @Schema(description = "Service type this profile applies to (e.g. VOICE, USAGE, SMS)", example = "VOICE")
    private String serviceType;

    @Schema(description = "Number of field mappings configured in this profile (populated in summary response only)", example = "12")
    private int mappingCount;

    @Schema(description = "Field mapping overrides belonging to this profile")
    private List<TapProfileFieldOverrideDTO> fieldMappings;
}
