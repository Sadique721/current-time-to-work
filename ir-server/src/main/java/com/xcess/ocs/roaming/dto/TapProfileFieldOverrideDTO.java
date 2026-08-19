package com.xcess.ocs.roaming.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "DTO for TAP profile field override — used for both request and response")
public class TapProfileFieldOverrideDTO {

    @Schema(description = "Unique identifier of this profile-field mapping link (populated in response only)", example = "501")
    private Long id;

    @NotNull
    @Schema(description = "ID of the master TapFieldMapping to include in this profile",
            example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long tapFieldMappingId;

    @Schema(description = "Human-readable logical name of the field (populated in response only)", example = "dialledDigits")
    private String fieldName;

    @Schema(description = "Dot-notation ASN.1 path (populated in response only)",
            example = "basicCallInformation.destination.dialledDigits")
    private String asnPath;

    @Schema(description = "Overrides the master default_value for this profile only", example = "99999999999999")
    private String customDefaultValue;

    @Schema(description = "Overrides the master is_mandatory flag for this profile only. Null means use master value.", example = "true")
    private Boolean isMandatoryOverride;
}
