package com.xcess.ocs.dto.search;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Search criteria for Organization entity.
 * Used for paginated search with optional search term.
 */
@Data
@NoArgsConstructor
@Schema(
        name = "Organization SearchDTO",
        description = "Schema to hold Organization search details"
)
public class OrganizationSearchDTO {
    
    /**
     * Search term to match against organization name, suffix name, legal name, or address.
     * Case-insensitive partial match.
     */
    @Schema(
            description = "Search term for organization name, suffix name, legal name, or address",
            example = "Tech",
            nullable = true
    )
    private String searchTerm;
}