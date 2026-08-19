package com.xcess.ocs.roaming.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class RoamingRatedSummaryViewDTO {
    private Long summaryId;
    private LocalDate summaryDate;
    private String tapFileName;
    private Long tapFileId;
    private String tapDirection;
    private String tapVersion;
    private String partnerName;
    private String serviceType;
    /** Zone name resolved via ZonePrefixTrie during rating (null when prefix-based rating was used) */
    private String zoneName;
    /** Number of VOICE calls in this summary group */
    private Integer totalCalls;
    /** Total SMS message count (SUM of eventNos across SMS CDRs in this group) */
    private Integer totalSms;
    private Long totalDurationSec;
    private BigDecimal totalUsageBytes;
    private BigDecimal totalTapCharge;
    private BigDecimal ourCharge;
}
