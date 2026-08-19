package com.xcess.ocs.dto.search;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(
        name = "19. Country SearchDTO",
        description = "Schema to hold Country search details"
)
public class CountrySearchDTO {
        @Schema(
                description = "Search term for country name and country code",
                example = "US",
                nullable = true
        )
        private String searchTerm;
}
