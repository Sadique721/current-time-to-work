package com.xcess.ocs.dto;

import com.xcess.ocs.entity.PrefixType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for Prefix entity.
 * Used for API requests and responses.
 * 
 * @author OCS
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "5. Prefix" , description = "Schema to hold details of Prefix")
public class PrefixDTO {
    
    /** Unique identifier of the prefix (generated automatically) */
    @Schema(description = "ID of the prefix", accessMode = Schema.AccessMode.READ_ONLY)
    private Long prefixId;

    /** Name of the country this prefix belongs to */
    @Schema(
            description = "Name of the country",
            example = "Argentina"
    )
    @NotBlank(message = "Country name cannot be empty")
    private String countryName;

    /** Area/mobile code WITHOUT country code (e.g., "415", "91", "9876").
     * Only numbers allowed. */
    @Schema(
            description = "Prefix number (numbers only, without country code)",
            example = "5478"
    )
    @NotBlank(message = "Prefix cannot be empty")
    @Pattern(regexp = "^\\d{1,15}$", message = "Invalid prefix format. It must contain only numbers (e.g., '91', '415', '23801').")
    private String prefix;

    /** Descriptive name for the prefix (letters, spaces, hyphens only - no numbers) */
    @Schema(
            description = "Prefix name (letters, spaces, hyphens only)",
            example = "California"
    )
    @NotBlank(message = "Prefix name cannot be empty")
    @Size(min = 2, max = 50, message = "Prefix name must be between 2 and 50 characters")
    @Pattern(regexp = "^[A-Za-z -]{2,50}$", message = "Invalid prefix name. Only letters, spaces, and hyphens are allowed.")
    private String prefixName;

    @Schema(
            description = "Type of prefix: INTERCONNECT for area/mobile codes, ROAMING for PLMN prefixes",
            example = "INTERCONNECT"
    )
    @NotNull(message = "Prefix type is required")
    private PrefixType prefixType = PrefixType.INTERCONNECT;
}