package com.xcess.ocs.dto.search;

import com.xcess.ocs.entity.ServiceType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(
        name = "Rate Package SearchDTO",
        description = "Schema to hold Rate Package search details"
)
public class RatePackageSearchDTO {
    @Schema(
            description = "Search term for global search",
            example = "Basic Package",
            nullable = true
    )
    private String searchTerm;

    @Schema(
            description = "Service type to filter",
            example = "VOICE",
            allowableValues = {"VOICE", "SMS", "USAGE"},
            nullable = true
    )
    private ServiceType serviceType;
}