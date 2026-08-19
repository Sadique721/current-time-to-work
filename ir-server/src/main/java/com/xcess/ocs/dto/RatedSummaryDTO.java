package com.xcess.ocs.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatedSummaryDTO {
    private Long summaryId;
    private LocalDate summaryDate;

    private String accountCode;
    private Long partnerId;
    private String partnerName;

    private String sourcePrefix;
    private String sourcePrefixName;
    private String sourceCountryCode;
    private String sourceCountryName;

    private String destinationPrefix;
    private String destinationPrefixName;
    private String destinationCountryCode;
    private String destinationCountryName;

    private String zoneName;
    private String ratingType;

    private String serviceType;
    private String direction;

    private BigDecimal appliedRate;

    /** Count of VOICE CDRs. Null for SMS and USAGE rows. */
    private Long totalCalls;

    /** Sum of event_nos from SMS CDRs. Null for VOICE and USAGE rows. */
    private Long totalSms;

    /** Count of USAGE session CDRs. Null for VOICE and SMS rows. */
    private Long totalSessions;

    private BigDecimal totalDuration;
    private BigDecimal totalCharge;
    private BigDecimal totalDataVolume;
}
