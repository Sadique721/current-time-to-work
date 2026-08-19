package com.xcess.ocs.dto;

import com.xcess.ocs.entity.LineOfBusiness;
import com.xcess.ocs.entity.ServiceType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsageRatedCdrDTO {
    private String subscriberIdentity;
    private String accessPointName;
    private String startTime;
    private String endTime;
    
    // INTERCONNECT-specific fields
    private String incomingAccountId;
    private String outgoingAccountId;

    // ROAMING-specific fields
    private String homePlmn;
    private String visitedPlmn;

    private ServiceType serviceType;
    private LineOfBusiness lineOfBusiness;
    
    private Double totalUsage;
    private Double uploadUsage;
    private Double downloadUsage;
    private String measurementUnit;
}
