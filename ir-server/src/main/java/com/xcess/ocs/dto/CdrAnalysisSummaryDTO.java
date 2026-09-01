package com.xcess.ocs.dto;

import com.xcess.ocs.entity.LineOfBusiness;
import com.xcess.ocs.entity.RatingStatus;
import com.xcess.ocs.entity.ServiceType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Summary DTO containing essential 5 fields for compact CDR Analysis UI table listing.")
public class CdrAnalysisSummaryDTO {

    @Schema(description = "CDR Record ID", example = "101")
    private Long id;

    @Schema(description = "Service Type (VOICE, SMS, USAGE)", example = "VOICE")
    private ServiceType serviceType;

    @Schema(description = "Line of Business (INTERCONNECT, ROAMING)", example = "INTERCONNECT")
    private LineOfBusiness lineOfBusiness;

    @Schema(description = "Incoming Rating Status", example = "RATED")
    private RatingStatus incomingRatingStatus;

    @Schema(description = "Outgoing Rating Status", example = "RATED")
    private RatingStatus outgoingRatingStatus;
}
