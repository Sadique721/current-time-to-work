package com.xcess.ocs.dto.search;

import com.xcess.ocs.entity.LineOfBusiness;
import com.xcess.ocs.entity.RatingStatus;
import com.xcess.ocs.entity.ServiceType;
import com.xcess.ocs.roaming.entity.CallType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "Filter criteria for CDR export.")
public class CdrFilterRequest {

    // Voice / SMS: callingNumber; Usage: subscriberIdentity
    @Schema(description = "Calling number (Voice/SMS) or Subscriber Identity (Usage)", example = "919876543210")
    private String callingOrSubscriber;

    // Voice / SMS: calledNumber; Usage: accessPointName
    @Schema(description = "Called number (Voice/SMS) or Access Point Name (Usage)", example = "internet")
    private String calledOrApn;

    @Schema(description = "Incoming account ID", example = "ACC-001")
    private String incomingAccountId;

    @Schema(description = "Outgoing account ID", example = "ACC-002")
    private String outgoingAccountId;

    @Schema(description = "Incoming rating status", example = "RATED")
    private RatingStatus incomingRatingStatus;

    @Schema(description = "Outgoing rating status", example = "RATED")
    private RatingStatus outgoingRatingStatus;

    @Schema(description = "Home PLMN", example = "40401")
    private String homePlmn;

    @Schema(description = "Visited PLMN", example = "23420")
    private String visitedPlmn;

    @Schema(description = "Zone name", example = "ZONE_EU")
    private String zoneName;

    @Schema(description = "Line of business", example = "ROAMING")
    private LineOfBusiness lineOfBusiness;

    @Schema(description = "Service type", example = "VOICE")
    private ServiceType serviceType;

    @Schema(description = "Call type", example = "MO_VOICE")
    private CallType callType;

    /**
     * Voice/Usage: filters on start_time. SMS: filters on created_date.
     */
    @Schema(description = "Start of time range (ISO date-time). For SMS this applies to createdDate.", example = "2024-01-01T00:00:00")
    private LocalDateTime fromTime;

    @Schema(description = "End of time range (ISO date-time). For SMS this applies to createdDate.", example = "2024-12-31T23:59:59")
    private LocalDateTime toTime;
}
