package com.xcess.ocs.roaming.dto;

import com.xcess.ocs.entity.ServiceType;
import com.xcess.ocs.roaming.entity.CallType;
import com.xcess.ocs.roaming.entity.RoamingRatingStatus;
import com.xcess.ocs.roaming.entity.TapDirection;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class RoamingRatedCdrDTO {
    private Long roamingCdrId;
    private Long tapFileId;
    private String fileName;
    private Long partnerId;
    private String partnerCode;
    private TapDirection tapDirection;
    private CallType callType;
    private String imsi;
    private String msisdn;
    private String callingNumber;
    private String calledNumber;
    private LocalDateTime callStartTime;
    private Integer callDurationSec;
    private String visitedPlmn;
    private String homePlmn;
    private ServiceType serviceType;
    private BigDecimal tapCharge;
    private String currency;
    private String zoneName;
    private Long ratePackageId;
    private String ratePackageName;
    private Long rateDetailId;
    private BigDecimal appliedRate;
    private BigDecimal ourCharge;
    private RoamingRatingStatus ratingStatus;
    private String ratingFailureReason;
}
