package com.xcess.ocs.dto;

import com.xcess.ocs.entity.LineOfBusiness;
import com.xcess.ocs.entity.RatingStatus;
import com.xcess.ocs.entity.ServiceType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO representing unified CDR analysis details across Voice, SMS, and Usage transactions.")
public class CdrAnalysisDTO {

    @Schema(description = "CDR Record ID", example = "101")
    private Long id;

    @Schema(description = "Service Type (VOICE, SMS, USAGE)", example = "VOICE")
    private ServiceType serviceType;

    @Schema(description = "Line of Business (INTERCONNECT, ROAMING)", example = "INTERCONNECT")
    private LineOfBusiness lineOfBusiness;

    @Schema(description = "Calling Number (Voice/SMS) or Subscriber Identity (Usage)", example = "919820012345")
    private String callingOrSubscriber;

    @Schema(description = "Called Number (Voice/SMS) or Access Point Name (Usage)", example = "919810054321")
    private String calledOrApn;

    @Schema(description = "Start Time of transaction", example = "2026-08-06T10:00:00")
    private LocalDateTime startTime;

    @Schema(description = "End Time of transaction", example = "2026-08-06T10:05:00")
    private LocalDateTime endTime;

    @Schema(description = "Duration in seconds (Voice), Event count (SMS), or Volume in bytes (Usage)", example = "300")
    private String durationOrUsage;

    @Schema(description = "Incoming Account ID", example = "ACC-JIO")
    private String incomingAccountId;

    @Schema(description = "Outgoing Account ID", example = "ACC-AIRTEL")
    private String outgoingAccountId;

    @Schema(description = "Incoming Rating Status", example = "RATED")
    private RatingStatus incomingRatingStatus;

    @Schema(description = "Outgoing Rating Status", example = "RATED")
    private RatingStatus outgoingRatingStatus;

    @Schema(description = "Incoming Total Cost", example = "1.5000")
    private BigDecimal incomingTotalCost;

    @Schema(description = "Outgoing Total Cost", example = "0.7500")
    private BigDecimal outgoingTotalCost;

    @Schema(description = "Home PLMN", example = "40445")
    private String homePlmn;

    @Schema(description = "Visited PLMN", example = "23415")
    private String visitedPlmn;

    @Schema(description = "Matched Zone Name", example = "India Mobile Zone")
    private String zoneName;

    @Schema(description = "Call Type (MO_VOICE, MT_VOICE, GPRS, etc.)", example = "MO_VOICE")
    private String callType;

    @Schema(description = "Timestamp when CDR was rated", example = "2026-08-06T10:05:01")
    private LocalDateTime ratedAt;

    @Schema(description = "Timestamp when CDR record was created", example = "2026-08-06T10:00:00")
    private LocalDateTime createdDate;

    @Schema(description = "Timestamp when CDR record was last modified", example = "2026-08-06T10:05:00")
    private LocalDateTime modifiedDate;
}
