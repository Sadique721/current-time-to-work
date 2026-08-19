package com.xcess.ocs.dto;

import com.xcess.ocs.entity.PrefixType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ZonePrefixOption", description = "A single selectable item in the zone prefix dropdown")
public class ZonePrefixOptionDTO {

    @Schema(description = "prefixId if sourceType=PREFIX, countryId if sourceType=COUNTRY")
    private Long id;

    @Schema(description = "Human-readable label shown in the dropdown", example = "India - Mumbai (415)")
    private String label;

    @Schema(description = "Actual digit string stored in prefixPattern", example = "415")
    private String value;

    @Schema(description = "PREFIX = from prefix table, COUNTRY = from country table", example = "PREFIX")
    private String sourceType;

    @Schema(description = "INTERCONNECT or ROAMING — null for COUNTRY entries", example = "INTERCONNECT")
    private PrefixType prefixType;

    @Schema(description = "Country name for grouping/display", example = "India")
    private String countryName;

    @Schema(description = "Country calling code", example = "91")
    private String countryCode;
}
