package com.xcess.ocs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Data Transfer Object for Country entity.
 * Used for API requests and responses.
 * 
 * @author OCS
 * @version 1.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "4. Country", description = "Country details to store different rates for different countries")
public class CountryDTO {
    
    /** Unique identifier of the country (generated automatically) */
    @Schema(
            description = "ID of the country",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private Long countryId;

    /** Name of the country - must be unique */
    @Schema(
            description = "Name of the country",
            example = "India"
    )
    @NotBlank(message = "Country name cannot be empty")
    @Pattern(regexp = "^[A-Z][a-zA-Z ]*$", message = "Country name must start with a capital letter and contain only letters and spaces")
    private String name;

    /** Country code without + prefix (e.g., "1", "91", "44").
     * Duplicate codes are allowed (e.g., USA and Canada both use "1") */
    @Schema(
            description = "Country code (without + prefix)",
            example = "91"
    )
    @NotBlank(message = "Country code cannot be empty")
    private String countryCode;

    /** ISO 4217 currency code */
    @Schema(description = "Currency code (ISO 4217)", example = "USD")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency code must be 3 uppercase letters")
    @NotBlank(message = "Currency code cannot be empty")
    private String currencyCode;

    /** Currency symbol */
    @Schema(description = "Currency symbol", example = "$")
    @NotBlank(message = "Currency symbol cannot be empty")
    private String currencySymbol;

    /** ISO 3166 alpha-2 country code */
    @Schema(
            description = "ISO 3166 alpha-2 country code",
            example = "IN"
    )
    @NotBlank(message = "ISO code cannot be empty")
    @Pattern(regexp = "^[A-Z]{2}$", message = "ISO code must be exactly 2 uppercase letters")
    private String isoCode;
}
