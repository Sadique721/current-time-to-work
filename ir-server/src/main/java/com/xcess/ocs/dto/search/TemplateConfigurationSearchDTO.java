package com.xcess.ocs.dto.search;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Search criteria for Template Configuration entity.
 * Used for paginated search with optional search term.
 */
@Data
@NoArgsConstructor
@Schema(
        name = "TemplateConfiguration SearchDTO",
        description = "Schema to hold Template Configuration search details"
)
public class TemplateConfigurationSearchDTO {
    
    /**
     * Search term to match against template name, template description.
     * Case-insensitive partial match.
     */
    @Schema(
            description = "Search term for template name or template description",
            example = "Corporate",
            nullable = true
    )
    private String searchTerm;
}