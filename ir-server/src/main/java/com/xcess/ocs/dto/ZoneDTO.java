package com.xcess.ocs.dto;

import com.xcess.ocs.entity.PrefixInputMode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "Zone", description = "Schema to hold zone details for INTERCONNECT and ROAMING rating")
public class ZoneDTO {

    @Schema(description = "ID of the zone", accessMode = Schema.AccessMode.READ_ONLY)
    private Long zoneId;

    @Schema(description = "Human-readable zone name", example = "ZONE_EU")
    @NotBlank(message = "Zone name is required")
    private String zoneName;

    @Schema(
        description = "Normalized comma-separated prefixes stored after create/update. " +
                      "In MANUAL mode: populated from the raw input after validation. " +
                      "In DROPDOWN mode: resolved by backend from selectedPrefixIds and selectedCountryIds. " +
                      "Always returned in GET responses.",
        example = "23801,23802,26201",
        accessMode = Schema.AccessMode.READ_ONLY
    )
    private String prefixPattern;

    @Schema(description = "Description of the zone", example = "European Union countries")
    private String description;

    @Schema(description = "Priority for zone conflict resolution (lower wins)", example = "100")
    @NotNull(message = "Priority is required")
    private Integer priority = 100;

    @Schema(
        description = "How prefixes were entered. MANUAL = typed by admin. DROPDOWN = selected from prefix/country list.",
        example = "MANUAL"
    )
    @NotNull(message = "Prefix input mode is required")
    private PrefixInputMode prefixInputMode = PrefixInputMode.MANUAL;

    @Schema(
        description = "Raw comma-separated prefix string. Required when prefixInputMode is MANUAL. " +
                      "Ignored when prefixInputMode is DROPDOWN.",
        example = "23801,23802,26201"
    )
    private String rawPrefixPattern;

    @Schema(
        description = "List of Prefix table IDs to include. Used when prefixInputMode is DROPDOWN. " +
                      "Transient — not stored, resolved to prefixPattern by backend.",
        example = "[1, 5, 12]"
    )
    private List<Long> selectedPrefixIds;

    @Schema(
        description = "List of Country IDs whose countryCode values are included. Used when prefixInputMode is DROPDOWN. " +
                      "Transient — not stored, resolved to prefixPattern by backend.",
        example = "[23, 47]"
    )
    private List<Long> selectedCountryIds;
}
