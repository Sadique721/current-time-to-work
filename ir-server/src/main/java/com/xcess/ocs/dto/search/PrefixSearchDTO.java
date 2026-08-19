package com.xcess.ocs.dto.search;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(
        name = "20. Prefix SearchDTO",
        description = "Schema to hold Prefix search details"
)
public class PrefixSearchDTO {
    @Schema(
            description = "Search term that will be matched against prefix, prefixName and country name",
            example = "india",
            nullable = true
    )
    private String searchTerm;

//     @Schema(
//             description = "Search specific fields (prefix, prefixName, countryName)",
//             example = "['prefix', 'countryName']",
//             nullable = true
//     )
//     private List<String> searchFields;
}