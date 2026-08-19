package com.xcess.ocs.roaming.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(
        name = "TapProfileSearchDTO",
        description = "Search/filter criteria for TAP profiles. All fields are optional."
)
public class TapProfileSearchDTO {

    @Schema(description = "Partial match on profileName (case-insensitive).",
            example = "EU", nullable = true)
    private String profileName;

    @Schema(description = "Partial match on description (case-insensitive).",
            example = "roaming", nullable = true)
    private String description;

    @Schema(description = "Filter by active status. Null returns both active and inactive profiles.",
            example = "true", nullable = true)
    private Boolean isActive;
}
