package com.xcess.ocs.roaming.dto;

import com.xcess.ocs.entity.ServiceType;
import com.xcess.ocs.roaming.entity.TapDirection;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@Schema(
        name = "RoamingRatedSummary SearchDTO",
        description = "Schema to hold RoamingRatedSummary search details"
)
public class RoamingRatedSummarySearchDTO {

    @Schema(description = "Filter by summary date", example = "2026-05-15", nullable = true)
    private LocalDate summaryDate;

    @Schema(description = "Filter by TAP file name (partial match)", example = "TDINDXX", nullable = true)
    private String fileName;

    @Schema(description = "Filter by tap direction", example = "TAP_IN", nullable = true)
    private TapDirection tapDirection;

    @Schema(description = "Filter by TAP version", example = "TAP3.12", nullable = true)
    private String tapVersion;

    @Schema(description = "Filter by partner name (partial match)", example = "Vodafone", nullable = true)
    private String partnerName;

    @Schema(description = "Filter by partner ID", example = "1", nullable = true)
    private Long partnerId;

    @Schema(description = "Filter by service type", example = "VOICE_MO", nullable = true)
    private ServiceType serviceType;
}
