package com.xcess.ocs.dto;

import com.xcess.ocs.entity.LineOfBusiness;
import com.xcess.ocs.entity.ServiceType;
import com.xcess.ocs.roaming.entity.CallType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a raw SMS CDR message coming from Kafka before mapping fields.
 * Fields correspond to those in Source_CDR_configuration table.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SmsRatedCdrDTO {
    // Common fields for both INTERCONNECT and ROAMING
    private String startTime;
    private String endTime;
    private String callingNumber;
    private String calledNumber;

    // INTERCONNECT-specific fields
    private String incomingAccountId;
    private String outgoingAccountId;

    // ROAMING-specific fields
    private String homePlmn;
    private String visitedPlmn;

    /** Call direction from CDR (MO_SMS, MT_SMS etc.) — metadata */
    private CallType callType;

    /** Service type from CDR (SMS) */
    private ServiceType serviceType;

    // LINE OF BUSINESS (set by MessageProcessor before rating)
    private LineOfBusiness lineOfBusiness;

    // SMS fields for rating calculations
    private String eventNos;
}
