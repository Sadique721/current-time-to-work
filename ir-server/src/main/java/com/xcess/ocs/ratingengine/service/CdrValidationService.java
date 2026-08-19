package com.xcess.ocs.ratingengine.service;

import com.xcess.ocs.entity.LineOfBusiness;
import com.xcess.ocs.entity.RateableCdr;
import com.xcess.ocs.entity.ServiceType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Service responsible for validating CDRs before rating processing.
 * Separating this logic keeps the rating engine clean and focused on rating.
 */
@Service
public class CdrValidationService {

    /**
     * Validates a CDR before rating.
     * @param cdr The CDR to validate
     * @param serviceType The service type
     * @return A String indicating the failure reason, or null if valid.
     */
    public String validateCdr(RateableCdr cdr, ServiceType serviceType) {
        // 1. Common Validation
        // SMS CDRs have no timestamps by design — skip this check for SMS
        if (!ServiceType.SMS.equals(serviceType) && cdr.getStartTime() == null) {
            return "INVALID_TIMESTAMP";
        }

        // 2. Line of Business (LOB) Bifurcation Validation
        if (LineOfBusiness.INTERCONNECT.equals(cdr.getLineOfBusiness())) {
            if ((cdr.getIncomingAccountId() == null || cdr.getIncomingAccountId().isBlank()) &&
                (cdr.getOutgoingAccountId() == null || cdr.getOutgoingAccountId().isBlank())) {
                return "MISSING_ACCOUNT_ID";
            }
        } else if (LineOfBusiness.ROAMING.equals(cdr.getLineOfBusiness())) {
            if (cdr.getHomePlmn() == null || cdr.getHomePlmn().isBlank()) {
                return "MISSING_HOME_PLMN";
            }
            if (cdr.getVisitedPlmn() == null || cdr.getVisitedPlmn().isBlank()) {
                return "MISSING_VISITED_PLMN";
            }
        }

        // 3. Service Type Bifurcation Validation
        if (ServiceType.VOICE.equals(serviceType)) {
            if (cdr.getCallingNumber() == null || cdr.getCallingNumber().isBlank()) {
                return "MISSING_CALLING_NUMBER";
            }
            if (cdr.getCalledNumber() == null || cdr.getCalledNumber().isBlank()) {
                return "MISSING_CALLED_NUMBER";
            }
            if (cdr.getEndTime() == null) {
                return "MISSING_END_TIME";
            }
            if (cdr.getEndTime().isBefore(cdr.getStartTime())) {
                return "INVALID_DURATION";
            }
        } else if (ServiceType.SMS.equals(serviceType)) {
            if (cdr.getCallingNumber() == null || cdr.getCallingNumber().isBlank()) {
                return "MISSING_CALLING_NUMBER";
            }
            if (cdr.getCalledNumber() == null || cdr.getCalledNumber().isBlank()) {
                return "MISSING_CALLED_NUMBER";
            }
            if (cdr.getEventCountForRating() == null || cdr.getEventCountForRating() <= 0) {
                return "INVALID_MESSAGE_COUNT";
            }
        } else if (ServiceType.USAGE.equals(serviceType)) {
            if (cdr.getCallingNumber() == null || cdr.getCallingNumber().isBlank()) {
                return "MISSING_SUBSCRIBER_IDENTITY";
            }
            if (cdr.getCalledNumber() == null || cdr.getCalledNumber().isBlank()) {
                return "MISSING_ACCESS_POINT_NAME";
            }
            if (cdr.getUsageAmountForRating() == null || cdr.getUsageAmountForRating().compareTo(BigDecimal.ZERO) < 0) {
                return "INVALID_USAGE_AMOUNT";
            }
            if (cdr.getMeasurementUnitForRating() == null || cdr.getMeasurementUnitForRating().isBlank()) {
                return "MISSING_MEASUREMENT_UNIT";
            }
        }

        return null;
    }
}
