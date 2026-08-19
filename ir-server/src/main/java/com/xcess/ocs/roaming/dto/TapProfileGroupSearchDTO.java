package com.xcess.ocs.roaming.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "Search criteria for TAP profile groups. All fields are optional.")
public class TapProfileGroupSearchDTO {

    @Schema(description = "Partial match on name (case-insensitive)", example = "EU")
    private String name;

    @Schema(description = "Partial match on description (case-insensitive)", example = "roaming")
    private String description;

    @Schema(description = "Filter by active status. Null returns both.", example = "true")
    private Boolean isActive;
}
