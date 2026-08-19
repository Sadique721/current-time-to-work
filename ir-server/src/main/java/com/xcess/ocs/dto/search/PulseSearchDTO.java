package com.xcess.ocs.dto.search;

import com.xcess.ocs.entity.ServiceType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(
    name = "21. Pulse SearchDTO",
    description = "Schema to hold Pulse search details"
)
public class PulseSearchDTO {
    @Schema(
        description = "Search term for pulseName, unit and noOfUnits",
        example = "Voice_60Sec",
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
