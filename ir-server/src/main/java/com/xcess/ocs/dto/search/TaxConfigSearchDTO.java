package com.xcess.ocs.dto.search;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(name = "TaxConfig SearchDTO", description = "Search criteria for filtering tax configurations")
public class TaxConfigSearchDTO {

    @Schema(description = "Search term for country code, tax type, tax name, or region code",
            example = "GST", nullable = true)
    private String searchTerm;
}
