package com.xcess.ocs.dto.search;

import com.xcess.ocs.entity.PrefixInputMode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(name = "Zone SearchDTO", description = "Schema to hold zone search criteria")
public class ZoneSearchDTO {

    @Schema(description = "Zone name to search", example = "ZONE_EU", nullable = true)
    private String zoneName;

    @Schema(description = "Description to search", example = "European", nullable = true)
    private String description;

    @Schema(description = "Priority to filter", example = "100", nullable = true)
    private Integer priority;

    @Schema(description = "Filter by prefix input mode: MANUAL or DROPDOWN", example = "MANUAL", nullable = true)
    private PrefixInputMode prefixInputMode;
}
