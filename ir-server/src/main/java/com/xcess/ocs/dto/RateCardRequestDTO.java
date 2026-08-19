package com.xcess.ocs.dto;

import com.xcess.ocs.entity.RatePackageType;
import com.xcess.ocs.entity.ServiceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "18. Rate Card Request", description = "Request DTO for fetching rate card details")
public class RateCardRequestDTO {

    @Schema(description = "Rate Package ID to fetch rate card for", example = "1")
    @NotNull(message = "Rate Package ID is required")
    private Long ratePackageId;

    @Schema(description = "Service type for the rate card", allowableValues = {"VOICE", "USAGE", "SMS"}, example = "VOICE")
    @NotNull(message = "Service type is required")
    private ServiceType serviceType;

    @Schema(
            description = "Source Country code",
            example = "91"
    )
    private String sourceCountryCode;

    @Schema(
            description = "Destination Country code",
            example = "265"
    )
    private String destinationCountryCode;
}
